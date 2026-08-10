package com.yurishelf.app

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.room.Room
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yurishelf.app.data.BlockWordStore
import com.yurishelf.app.data.CatalogRepository
import com.yurishelf.app.data.SeedCatalogImporter
import com.yurishelf.app.data.ai.AiSettingsStore
import com.yurishelf.app.data.ai.OpenAiClient
import com.yurishelf.app.data.local.YuriShelfDatabase
import com.yurishelf.app.data.remote.NetworkFactory
import com.yurishelf.app.data.remote.AccessTokenStore
import com.yurishelf.app.data.remote.KeystoreStringStore
import com.yurishelf.app.data.remote.ProxySettingsStore
import com.yurishelf.app.data.remote.SwitchableCallFactory

class YuriShelfApplication : Application(), ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(): ImageLoader = container.imageLoader
}

class AppContainer(context: Context) {
    private val preferences = context.getSharedPreferences("catalog-sync", Context.MODE_PRIVATE)
    private val proxySettingsStore = ProxySettingsStore(preferences)
    private val accessTokenStore = AccessTokenStore(preferences)
    private val blockWordStore = BlockWordStore(preferences)
    private val aiApiKeyStore = KeystoreStringStore(
        preferences = preferences,
        ciphertextKey = "ai_api_key_ciphertext",
        ivKey = "ai_api_key_iv",
        alias = "yurishelf_ai_api_key",
    )
    private val aiSettingsStore = AiSettingsStore(preferences, aiApiKeyStore)
    private val database = Room.databaseBuilder(
        context,
        YuriShelfDatabase::class.java,
        "yuri-shelf.db",
    )
        .addMigrations(
            YuriShelfDatabase.MIGRATION_1_2,
            YuriShelfDatabase.MIGRATION_2_3,
            YuriShelfDatabase.MIGRATION_3_4,
            YuriShelfDatabase.MIGRATION_4_5,
            YuriShelfDatabase.MIGRATION_5_6,
        )
        .build()

    private val seedCatalogImporter = SeedCatalogImporter(
        context = context,
        dao = database.subjectDao(),
        preferences = preferences,
    )

    private fun createHttpClient() = proxySettingsStore.get().let { proxySnapshot ->
        NetworkFactory.createHttpClient(
            userAgent = BuildConfig.BANGUMI_USER_AGENT,
            debug = BuildConfig.DEBUG,
            proxySettingsProvider = { proxySnapshot },
        )
    }

    private val networkCallFactory = SwitchableCallFactory(createHttpClient())
    private val api = NetworkFactory.createBangumiApi(networkCallFactory)
    private val aiClient = OpenAiClient(
        callFactory = networkCallFactory,
    )

    val imageLoader: ImageLoader = ImageLoader.Builder(context)
        .callFactory(networkCallFactory)
        .crossfade(false)
        .build()

    init {
        val resetNetwork = {
            networkCallFactory.replaceWith(createHttpClient())
        }
        proxySettingsStore.setOnChanged(resetNetwork)
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {
                    if (intent?.action == PROXY_CHANGE_ACTION) resetNetwork()
                }
            },
            IntentFilter(PROXY_CHANGE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    val repository = CatalogRepository(
        api = api,
        dao = database.subjectDao(),
        preferences = preferences,
        proxySettingsStore = proxySettingsStore,
        accessTokenStore = accessTokenStore,
        blockWordStore = blockWordStore,
        seedCatalogImporter = seedCatalogImporter,
        aiSettingsStore = aiSettingsStore,
        aiClient = aiClient,
    )

    private companion object {
        const val PROXY_CHANGE_ACTION = "android.intent.action.PROXY_CHANGE"
    }
}
