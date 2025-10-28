# StartupActivity Business Logic Analysis - Comprehensive Report

## Executive Summary

The StartupActivity is the app's initialization gateway that orchestrates multiple initialization flows upon app launch. It manages account authentication, idol data synchronization, payment verification, and extensive configuration loading through a combination of synchronous and asynchronous operations.

---

## 1. ARCHITECTURE OVERVIEW

### 1.1 Current Pattern (Old Implementation)
- **Activity-based**: `StartupActivity` is both UI controller and business logic handler
- **Thread-based**: Uses custom `StartupThread` for long-running operations
- **Observable pattern**: LiveData for event communication
- **Repository pattern**: Abstraction over API calls
- **UseCase pattern**: Encapsulation of business logic (Clean Architecture principles)

### 1.2 Modern Target Pattern (MVVM + Clean Architecture)
- **ViewModel-driven**: Separation of UI and business logic
- **Coroutines**: Structured concurrency with viewModelScope
- **Flow/State**: Reactive state management
- **Repository pattern**: Maintained for data access
- **UseCase pattern**: Enhanced with coroutine support

---

## 2. COMPLETE STARTUP FLOW

### 2.1 Application Launch Sequence

```
StartupActivity.onCreate()
    ↓
Initial Setup & Preferences
    ↓
Intent Handling (Deep Links, Share)
    ↓
startThread() → StartupThread.run()
    ↓
Account Check
    ├─ No Account → AuthActivity
    └─ Account Exists → API Initialization
        ↓
    getStartApi() → Multiple Concurrent API Calls
        ├─ getConfigSelf() [FIRST]
        ├─ getConfigStartup()
        ├─ getUpdateInfo()
        ├─ getAdTypeList()
        ├─ timeZoneUpdate()
        ├─ getUserSelf()
        ├─ getUserStatus()
        ├─ getMessageCoupon()
        ├─ getOfferWallReward() [if needed]
        ├─ getTypeList() [CELEB flavor only]
        └─ getBlocks() [if first time user]
            ↓
    Idol Data Synchronization
        ├─ Check update flags (all_idol_update, daily_idol_update)
        └─ Fetch updates or load from DB
            ↓
    Post-Completion Validations
        ├─ Check subscription status
        ├─ Check IAB (In-App Billing)
        └─ Navigate to MainActivity
```

---

## 3. DETAILED BUSINESS LOGIC

### 3.1 Activity Lifecycle & Initialization

**File**: `/Users/daewookoh/Desktop/Techplan/idol_android/old/app/src/main/java/net/ib/mn/activity/StartupActivity.kt`

#### onCreate() Flow (Lines 134-302)

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    // 1. Dark mode setup
    val darkMode = Util.getPreferenceInt(this, Const.KEY_DARKMODE, 
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(darkMode)
    
    // 2. ViewModel observation
    observeVM()
    
    // 3. Platform-specific initialization
    if (BuildConfig.CHINA) {
        PushyUtil.registerDevice(this, null)
        ChinaUtil.initNativeX(this)
    }
    
    // 4. UI setup
    window.statusBarColor = resources.getColor(R.color.text_white_black, theme)
    
    // 5. Preference initialization
    Util.setPreference(this, Const.AWARD_RANKING, 0L)
    Util.setPreference(this, Const.IS_VIDEO_SOUND_ON, false)
    
    // 6. Application instance setup
    IdolApplication.getInstance(this)
    
    // 7. Account retrieval
    account = IdolAccount.getAccount(this)
    mAccountLock = Any()
    
    // 8. Intent handling for deep links and shares
    handleIntents(intent)
    
    // 9. Animation preference setup
    val animation = Util.containsPreference(this, Const.PREF_ANIMATION_MODE)
    if (!animation) {
        Util.setPreference(this, Const.PREF_ANIMATION_MODE, 
            Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
    }
    
    // 10. Cache cleanup
    cacheDelete()
    
    // 11. Video cache management
    Util.videoCacheHide(this)
    
    // 12. Start initialization thread
    startThread()
    
    // 13. Server URL configuration
    var host = Util.getPreference(this, Const.PREF_SERVER_URL)
    if (!host.isNullOrEmpty()) {
        ServerUrl.HOST = host
        idolApiManager.reinit(...)
        accountManager.reinit(getUserSelfUseCase)
    }
    
    // 14. Deep link URL scheme handling for server switching
    handleDevScheme(intent)
    
    // 15. Remote config fetch
    RemoteConfigUtil.fetchRemoteConfig(this, account)
    
    // 16. Reset main screen preferences
    resetPreferenceMainScreen()
}
```

#### ViewModel Observation (Lines 304-318)

```kotlin
private fun observeVM() = with(viewModel) {
    // Triggered when activity recreation needed
    recreateActivity.observe(this@StartupActivity, SingleEventObserver {
        recreate()
    })
    
    // Progress update for UI
    updateProgress.observe(this@StartupActivity, SingleEventObserver { progress ->
        mProgress?.progress = progress.toInt()
    })
    
    // Main startup completion
    configLoadComplete.observe(this@StartupActivity, SingleEventObserver { success ->
        if(success) {
            goCompleteProcess()
        }
    })
}
```

---

### 3.2 Intent Handling (Deep Links & Share Intent)

**Lines 185-230**: Handles three intent types:

1. **Deep Link (auth/request)**:
   - Parse URI for authentication requests
   - Set flag: `is_auth_request`

2. **Text Share**:
   - Extract shared text
   - Route to WriteArticleActivity if user has favorite
   - Disable IAB check (`check_IAB = false`)

3. **Image Share**:
   - Extract image URI
   - Route to WriteArticleActivity if user has favorite
   - Disable IAB check

---

### 3.3 StartupThread & Account Initialization

**File**: StartupActivity.kt, Lines 383-479

```kotlin
private inner class StartupThread : Thread() {
    override fun run() {
        try {
            // 1. Get Google Advertising ID
            var adInfo: AdvertisingIdClient.Info? = null
            try {
                adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
            } catch (e: IOException) {
                // Handle Google Play Services unavailability
            }
            
            // 2. Save AD ID to preferences
            if (adInfo?.id != null) {
                Util.setPreference(this@StartupActivity, PROPERTY_AD_ID, adInfo.id)
            }
            
            // 3. Account check
            if (account == null) {
                account = IdolAccount.getAccount(this@StartupActivity)
            }
            
            // 4. No account → redirect to Auth
            if (account == null) {
                mNextIntent = AuthActivity.createIntent(this@StartupActivity)
                startNextActivity()
                finish()
            } else {
                // 5. Existing account → run startup APIs
                Util.setPreference(
                    this@StartupActivity,
                    Const.PREF_HEART_BOX_VIEWABLE,
                    true
                )
                
                viewModel.getInAppBanner(this@StartupActivity)
                
                // 6. Check if coming from OfferWall
                val isFromIdol = fromIdolIntent.getBooleanExtra(
                    if (BuildConfig.CELEB) 
                        Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_CELEB 
                    else 
                        Const.PARAM_INTENT_IN_HOUSE_OFFER_WALL_IDOL,
                    false
                )
                
                // 7. Trigger main startup API calls
                viewModel.getStartApi(
                    context = this@StartupActivity,
                    account = account,
                    isFromIdol = isFromIdol,
                    isUserBlockFirst = !Util.getPreferenceBool(
                        this@StartupActivity,
                        Const.USER_BLOCK_FIRST,
                        false
                    ),
                    to = rewardTo
                )
            }
        } catch (e: InterruptedException) {
            return
        }
    }
}
```

---

### 3.4 Main Startup API Orchestration

**File**: StartupViewModel.kt, Lines 177-223

```kotlin
fun getStartApi(
    context: Context,
    account: IdolAccount?,
    isFromIdol: Boolean,
    isUserBlockFirst: Boolean,
    to: String
) = viewModelScope.launch {
    // PHASE 1: Get config/self first (prerequisite for other APIs)
    async { getConfigSelf(context) }.await()
    
    // PHASE 2: Get startup config (critical path)
    val isStartupSuccess = async { getConfigStartup(context) }.await()
    if (!isStartupSuccess) {
        return@launch
    }
    
    // PHASE 3: Parallel API calls
    val tasks = listOf(
        async { getUpdateInfo(context) },                    // Idol update flags
        async { getAdTypeList(context) },                    // Ad types
        async { timeZoneUpdate(context) },                   // Timezone sync
        async { getConfigSelf(context) },                    // User preferences
        async { getMessageCoupon(context) },                 // Coupon messages
        async { getUserSelf(context, account) },             // User info
        async { getUserStatus(account) }                     // Tutorial status
    ).toMutableList()
    
    // Conditional tasks
    if (isFromIdol) {
        tasks.add(async { getOfferWallReward(to) })
    }
    
    if (BuildConfig.CELEB) {
        tasks.add(async { getTypeList(context) })
    }
    
    if (isUserBlockFirst) {
        tasks.add(async { getBlocks(context, "Y") })
    }
    
    // Calculate progress step
    val divisor = tasks.size
    progressStep = 100.0f / divisor
    
    // Wait for all tasks
    tasks.awaitAll()
    
    // Notify completion
    _configLoadComplete.value = Event(configLoadResult)
}
```

---

### 3.5 Key API Calls & Their Purposes

#### 3.5.1 getConfigStartup()
**Lines 364-469**

```kotlin
private suspend fun getConfigStartup(context: Context): Boolean {
    var isSuccess = false
    
    getConfigStartupUseCase().collectLatest { result ->
        if(!result.success) {
            return@collectLatest
        }
        
        isSuccess = true
        val gson = IdolGson.getInstance(false)
        
        // Save bad words list
        Util.setPreference(context, Const.BAD_WORDS, 
            gson.toJson(result.data?.badword))
        IdolAccount.badWords = gson.fromJson(
            Util.getPreference(context, Const.BAD_WORDS), 
            ArrayList<BadWordModel>::class.java
        )
        
        // Save board tags
        setTag("board_tag", gson.toJson(result.data?.boardTag), context)
        
        // LG Code (non-CELEB only)
        if (!BuildConfig.CELEB && ConfigModel.getInstance(context).showLg == "Y") {
            Util.setPreference(context, Const.LG_CODE, 
                gson.toJson(result.data?.lgcode))
        }
        
        // Official channels (non-CELEB only)
        if (!BuildConfig.CELEB) {
            Util.setPreference(context, Const.PREF_OFFICIAL_CHANNELS,
                gson.toJson(result.data?.sns))
        }
        
        // Notice list
        Util.setPreference(context, Const.PREF_NOTICE_LIST,
            result.data?.noticeList.toString())
        
        // Event list
        Util.setPreference(context, Const.PREF_EVENT_LIST,
            result.data?.eventList.toString())
        
        // Family app version list
        Util.setPreference(context, Const.PREF_FAMILY_APP_LIST,
            gson.toJson(result.data?.familyAppList))
        
        // Video upload specifications
        Util.setPreference(context, Const.PERF_UPLOAD_VIDEO_SPEC,
            gson.toJson(result.data?.uploadVideoSpec))
        
        // Exit popup configuration
        Util.setPreference(context, Const.PREF_END_POPUP,
            gson.toJson(result.data?.endPopup))
        
        // Help info
        Util.setPreference(context, Const.PREF_HELP_INFO,
            gson.toJson(result.data?.helpInfos))
        
        // New picks
        Util.setPreference(context, Const.PREF_NEW_PICKS,
            gson.toJson(result.data?.newPicks))
    }
    
    return isSuccess
}
```

**API Endpoint**: `GET /configs/startup/`

**Response Data**:
```json
{
    "success": true,
    "data": {
        "badword": [],              // Bad words filter list
        "boardTag": [],             // Board tag categories
        "lgcode": [],              // LG/Entertainment codes
        "sns": [],                 // Official SNS channels
        "noticeList": [],          // App notices
        "eventList": [],           // Current events
        "familyAppList": [],       // Related apps list
        "uploadVideoSpec": {},     // Video upload limits
        "endPopup": {},            // Exit popup config
        "newPicks": [],            // New voting picks
        "helpInfos": []            // Help information
    }
}
```

---

#### 3.5.2 getUpdateInfo()
**Lines 241-362**

```kotlin
private suspend fun getUpdateInfo(context: Context) {
    getUpdateInfoUseCase().collectLatest { result ->
        
        idolApiManager.startTimer()
        
        if (!result.success) {
            // Clear update flags on error
            Util.setPreference(context, Const.PREF_OFFICIAL_CHANNEL_UPDATE, "")
            Util.setPreference(context, Const.PREF_ALL_IDOL_UPDATE, "")
            Util.setPreference(context, Const.PREF_DAILY_IDOL_UPDATE, "")
            return@collectLatest
        }
        
        // Get stored update flags
        val allIdolUpdateFlag = Util.getPreference(
            context, Const.PREF_ALL_IDOL_UPDATE)
        val dailyIdolUpdateFlag = Util.getPreference(
            context, Const.PREF_DAILY_IDOL_UPDATE)
        val officialChannelUpdateFlag = Util.getPreference(
            context, Const.PREF_OFFICIAL_CHANNEL_UPDATE)
        
        idolApiManager.load()
        
        val allIdolUpdate = result.data?.allIdolUpdate
        val dailyIdolUpdate = result.data?.dailyIdolUpdate
        val officialChannelUpdate = result.data?.snsChannelUpdate
        
        // Check if official channel update needed
        if (!BuildConfig.CELEB) {
            if (officialChannelUpdateFlag == null || 
                !officialChannelUpdateFlag.equals(officialChannelUpdate, ignoreCase = true)) {
                Util.setPreference(context, 
                    Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL, true)
                Util.setPreference(context, 
                    Const.PREF_OFFICIAL_CHANNEL_UPDATE, officialChannelUpdate)
            } else {
                Util.setPreference(context, 
                    Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL, false)
            }
        }
        
        idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""
        idolApiManager.updateDaily { _updateProgress.value = Event(progress) }
        
        // Check all idol update
        if (allIdolUpdateFlag.isNullOrEmpty() || 
            !allIdolUpdateFlag.equals(allIdolUpdate, ignoreCase = true)) {
            // Full update needed
            idolApiManager.all_idol_update = allIdolUpdate ?: ""
            idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""
            idolApiManager.updateAll { _updateProgress.value = Event(progress) }
        } else if (dailyIdolUpdateFlag != dailyIdolUpdate) {
            // Daily update only
            idolApiManager.daily_idol_update = dailyIdolUpdate ?: ""
            idolApiManager.updateDaily { _updateProgress.value = Event(progress) }
        } else {
            // Load from DB
            loadIdolsFromDB(context, dailyIdolUpdateFlag, dailyIdolUpdate)
        }
    }
}
```

**API Endpoint**: `GET /update/`

**Response Data**:
```json
{
    "success": true,
    "data": {
        "all_idol_update": "2025-01-15T10:30:00Z",    // Full data update timestamp
        "daily_idol_update": "2025-01-15T14:20:00Z",  // Daily data update timestamp
        "sns_channel_update": "2025-01-10T08:00:00Z"  // SNS channel update timestamp
    }
}
```

---

#### 3.5.3 getUserSelf()
**Lines 620-653**

```kotlin
private suspend fun getUserSelf(context: Context, account: IdolAccount?) {
    account?.mDailyPackHeart = 0
    account?.saveAccount(context)
    
    val ts: Int = account?.userModel?.ts ?: 0
    
    getUserSelfUseCase(ts).collectLatest { result ->
        try {
            // Handle 401 Unauthorized
            if(result.code == 401) {
                configLoadResult = false
                account?.clearAccount(context)
                _recreateActivity.value = Event(true)
                return@collectLatest
            }
            
            // 304 Not Modified - use cached data
            if (result.code == 304) {
                Logger.v("ApiLogger", "users/self 304 ${result.message}")
            } else {
                // Update user info with fresh data
                result.data?.let {
                    account?.setUserInfo(context, it)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}
```

**API Endpoint**: `GET /users/self/?ts={timestamp}`

**Uses ETags for HTTP 304 caching**

**Response Data**:
```json
{
    "success": true,
    "objects": [
        {
            "id": 123,
            "email": "user@example.com",
            "nickname": "username",
            "level": 25,
            "diamond": 500,
            "strong_heart": 1000,
            "weak_heart": 500,
            "image_url": "https://...",
            "most": {
                "id": 456,
                "name": "Idol Name",
                "category": "M",
                "chart_codes": ["code1", "code2"],
                "type": "A"
            },
            "subscriptions": [],
            "ts": 1705315800
        }
    ]
}
```

---

#### 3.5.4 getConfigSelf()
**Lines 533-553**

```kotlin
private suspend fun getConfigSelf(context: Context) {
    withContext(Dispatchers.IO) {
        val result = getConfigSelfUseCase().first()
        if(!result.success) {
            handleError(result)
            return@withContext
        }
        Logger.v("ApiLogger", result.modelToString())
        
        // Parse config and store in ConfigModel singleton
        ConfigModel.getInstance(context).parse(result.data)
        
        // Check if award tab should be shown
        if (ConfigModel.getInstance(context).showAwardTab) {
            getAwardData(context)
        }
    }
    
    // Update progress on main thread
    withContext(Dispatchers.Main) {
        progress += progressStep
        _updateProgress.value = Event(progress)
    }
}
```

**API Endpoint**: `GET /configs/self/`

**Response Data** (JSON Object):
```json
{
    "cdn_url": "https://cdn.example.com/",
    "show_award_tab": true,
    "show_lg": "Y",
    "image_size_on_demand": "1024x1024",
    "...": "..."
}
```

---

#### 3.5.5 timeZoneUpdate()
**Lines 501-531**

```kotlin
private suspend fun timeZoneUpdate(context: Context) {
    val calendar = Calendar.getInstance()
    val currentTimeZone = calendar.timeZone.getDisplayName(
        false, TimeZone.LONG, Locale.ENGLISH)
    val storedTimeZone = Util.getPreference(context, Const.TIME_ZONE)
    
    val timeFormat = SimpleDateFormat("ZZZZZ", Locale.getDefault())
    val resultTime = timeFormat.format(System.currentTimeMillis())
    val timeZoneData = mapOf("timezone" to resultTime)
    
    updateTimeZoneUseCase(timeZoneData).collectLatest { result ->
        if(!result.success) {
            handleError(result)
            return@collectLatest
        }
        
        // Store current timezone
        if (!storedTimeZone.equals(currentTimeZone) || storedTimeZone == null) {
            Util.setPreference(context, Const.TIME_ZONE, currentTimeZone)
        }
    }
}
```

**API Endpoint**: `POST /users/timezone_update/`

**Request**: `{ "timezone": "+09:00" }`

---

#### 3.5.6 getAwardData()
**Lines 555-611**

```kotlin
private suspend fun getAwardData(context: Context) {
    getAwardDataUseCase().collectLatest { result ->
        val jsonString = Json.encodeToString(result)
        
        Util.setPreference(context, Const.AWARD_MODEL, jsonString)
        
        // Fetch idols for each chart
        getAllAwardsIdolList(context, result)
    }
}

private suspend fun getAllAwardsIdolList(context: Context, data: AwardModel?) {
    val chartModel = data?.charts
    val charCodes = chartModel?.map { it.code } ?: listOf()
    
    viewModelScope.launch(Dispatchers.IO) {
        deleteAllAwardsIdolUseCase().collectLatest {
            supervisorScope {
                charCodes.forEach { chartCode ->
                    if (chartCode == null) return@forEach
                    
                    launch {
                        getAwardIdolUseCase(chartCode).collect { result ->
                            if (!result.success) return@collect
                            
                            val idolList = gson.fromJson(
                                gson.toJson(result.data),
                                ArrayList<IdolModel>::class.java
                            )
                            
                            val idols = sort(context, idolList)
                            saveAwardsIdolUseCase(idols.map { it.toDomain() })
                        }
                    }
                }
            }
        }
    }
}
```

**API Endpoints**:
- `GET /awards/current/` - Get award chart definitions
- `GET /idols/?chart_code={code}` - Get idols for each chart

---

### 3.6 Security & Validation

#### 3.6.1 VM/Rooting Detection
**Lines 337-368**

```kotlin
override fun onPostResume() {
    super.onPostResume()
    
    tvTalkId = PreferenceManager.getDefaultSharedPreferences(this)
        .getString(Const.PREF_TV_TALK_ID, "") ?: ""
    
    // Detect VM/Rooting
    val vm = VMDetector.getInstance(this)
    vm.addPackageName("com.google.android.launcher.layouts.genymotion")
    vm.addPackageName("com.vphone.launcher")   // nox
    vm.addPackageName("com.bluestacks")        // bluestacks
    vm.addPackageName("com.bignox.app")        // nox
    vm.addPackageName("com.microvirt.launcher") // memu
    // ... more packages
    
    val isVM = vm.isVM()
    val isRooted = vm.isRooted
    val isX86 = vm.isx86Port()
    
    if (isVM && !DEBUG) {
        UtilK.postVMLog(this, lifecycleScope, miscRepository, 
            vm.toString(), Const.VM_DETECT_LOG)
        finish()
        return
    }
}
```

---

#### 3.6.2 Subscription Verification
**Lines 494-600**

```kotlin
@Throws(InterruptedException::class)
private fun checkSubscriptions() {
    account?.userModel?.subscriptions?.let { subscriptions ->
        if (subscriptions.isNotEmpty()) {
            val latch = CountDownLatch(subscriptions.size)
            
            for (subscription in subscriptions) {
                val jsonObject = JSONObject()
                if (subscription.familyappId == 1) {
                    val pkgName = if (BuildConfig.CELEB) 
                        "com.exodus.myloveactor" else "net.ib.mn"
                    
                    if (packageName.equals(pkgName, ignoreCase = true)) {
                        // Verify subscription on same platform
                        jsonObject.put("orderId", subscription.orderId)
                            .put("productId", subscription.skuCode)
                            .put("packageName", subscription.packageName)
                            .put("purchaseToken", subscription.purchaseToken)
                        
                        lifecycleScope.launch {
                            if (BuildConfig.ONESTORE) {
                                usersRepository.iabVerify(
                                    receipt = jsonObject.toString(),
                                    signature = "",
                                    itemType = SUBS,
                                    state = Const.IAB_STATE_ABNORMAL,
                                    listener = { response ->
                                        account?.mDailyPackHeart = 
                                            if (response.optInt("gcode") == 0) 
                                                response.optInt("heart") else 0
                                        account?.saveAccount(this@StartupActivity)
                                        latch.countDown()
                                    }
                                )
                            } else {
                                usersRepository.paymentsGoogleSubscriptionCheck(...)
                            }
                        }
                    } else {
                        // Platform mismatch warning
                        showWarningPlatformChange(latch, 
                            subscription.name,
                            R.string.warning_subscription_only_original)
                    }
                } else if (subscription.familyappId == 3) {
                    // Non-Android platform subscription
                    showWarningPlatformChange(latch,
                        subscription.name,
                        R.string.warning_subscription_platform_change_android)
                }
            }
            latch.await()
        }
    }
}
```

---

#### 3.6.3 IAB (In-App Billing) Verification
**Lines 603-742**

```kotlin
@Throws(InterruptedException::class)
private fun checkIAB() {
    val latch = CountDownLatch(1)
    
    lifecycleScope.launch {
        usersRepository.getIabKey({ response ->
            if (response.optBoolean("success")) {
                val key = response.optString("key")
                val pKey = checkKey(key)  // Decrypt public key
                
                if (mBillingManager == null) {
                    mBillingManager = BillingManager(
                        this@StartupActivity,
                        pKey,
                        object : BillingManager.BillingUpdatesListener {
                            override fun onBillingClientSetupFinished() {
                                // Query unconsumed purchases
                                mBillingManager?.queryPurchases()
                            }
                            
                            override fun onPurchasesUpdated(
                                billingResult: BillingResult,
                                purchases: List<Purchase>?
                            ) {
                                if (billingResult.responseCode == OK && purchases != null) {
                                    for (purchase in purchases) {
                                        if (!mBillingManager!!.isSubscription(purchase)) {
                                            // Consume in-app purchase
                                            mBillingManager!!.consumeAsync(
                                                purchase.purchaseToken,
                                                purchase.developerPayload,
                                                purchase
                                            )
                                            
                                            // Verify with server
                                            lifecycleScope.launch {
                                                if (BuildConfig.ONESTORE) {
                                                    usersRepository.iabVerify(
                                                        receipt = purchase.originalJson,
                                                        signature = purchase.signature,
                                                        itemType = INAPP,
                                                        state = Const.IAB_STATE_ABNORMAL
                                                    )
                                                } else {
                                                    usersRepository.paymentsGoogleItem(
                                                        receipt = purchase.originalJson,
                                                        signature = purchase.signature,
                                                        itemType = INAPP,
                                                        state = Const.IAB_STATE_ABNORMAL
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                latch.countDown()
                            }
                        })
                }
            }
        }, {
            latch.countDown()
        })
    }
    
    latch.await()
}

private fun checkKey(key: String): String {
    val key1 = key.substring(key.length - 7)
    val data = key.substring(0, key.length - 7)
    val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
    return String(pKey)
}
```

**API Endpoints**:
- `GET /users/iab_key/` - Get encrypted IAB public key
- `POST /users/iab_verify/` - Verify in-app purchases
- `POST /users/payments/google_item/` - Verify Google Play items
- `POST /users/payments/google_subscription_check/` - Verify Google Play subscriptions

---

### 3.7 Idol Data Synchronization

**File**: IdolApiManager.kt

```kotlin
fun startTimer() {
    updateJob?.cancel()
    updateJob = scope.launch {
        // Aggregation time check
        if (isAggregationTimeJustPassed()) {
            val randomDelay = 3000L
            delay(randomDelay)
            update()
        }
        
        // Periodic updates
        while (isActive) {
            if (ConfigModel.getInstance(context).cdnUrl.isNullOrEmpty()) {
                delay(interval * 1000)
                continue
            }
            
            if (updateAllNeeded || updateDailyNeeded) {
                logV("updateAllNeeded: $updateAllNeeded updateDailyNeeded: $updateDailyNeeded")
                update()
            } else if (!isConnected()) {
                logD("UDP server is gone... use legacy")
                update()
            }
            
            delay(interval * 1000)
        }
    }
}

// Update strategies based on flags
fun updateAll(callback: () -> Unit) {
    // Fetch complete idol list
    // Update heart, top3, images
    // Save to Room database
    callback()
}

fun updateDaily(callback: () -> Unit) {
    // Fetch anniversary/burning_day data
    // Update only necessary fields
    // Save to Room database
    callback()
}
```

---

### 3.8 Completion & Navigation

**Lines 900-1002**

```kotlin
private fun goCompleteProcess() = lifecycleScope.launch(Dispatchers.IO) {
    
    // Set default category if needed
    if (!Util.containsPreference(this@StartupActivity, 
        Const.PREF_DEFAULT_CATEGORY)) {
        account?.let {
            val category = when {
                it.most == null || it.most?.type.equals("B", ignoreCase = true) -> "M"
                else -> it.most?.category
            }
            Util.setPreference(this@StartupActivity, 
                Const.PREF_DEFAULT_CATEGORY, category)
        }
    } else if (!BuildConfig.CELEB) {
        // Update category if favorite changed
        account?.most?.let {
            if (!Util.getPreference(this@StartupActivity, 
                Const.PREF_DEFAULT_CATEGORY).equals(it.category)) {
                val category = if (it.category == "B") "M" else it.category
                Util.setPreference(this@StartupActivity, 
                    Const.PREF_DEFAULT_CATEGORY, category)
            }
        }
    }
    
    IdolApplication.STRATUP_CALLED = true
    
    val checkGoPush = intent.getBooleanExtra("go_push_start", false)
    
    // Handle push notification
    if (checkGoPush) {
        withContext(Dispatchers.Main) {
            setResult(RESULT_OK)
            finish()
        }
        return@launch
    }
    
    // Validate user info
    if (account == null || (account != null && !account!!.hasUserInfo())) {
        Handler(Looper.getMainLooper()).post {
            mNextIntent = AuthActivity.createIntent(this@StartupActivity)
            startNextActivity()
            finish()
        }
        return@launch
    }
    
    // Handle deep links
    val nextIntentForLink: Intent? = intent.getParcelableExtra(PARAM_NEXT_INTENT)
    if (nextIntentForLink != null) {
        mNextIntent = MainActivity.createIntentFromDeepLink(
            this@StartupActivity,
            false,
            isDeepLinkClickFromIdol()
        )
        mNextIntent?.putExtra(PARAM_NEXT_INTENT, nextIntentForLink)
        
        withContext(Dispatchers.Main) {
            startNextActivity()
            finish()
        }
    }
    
    // Subscription and IAB checks
    if (!BuildConfig.ONESTORE) {
        checkSubscriptions()
    }
    
    if (check_IAB) {
        checkIAB()
    }
    
    // Navigate to main
    if (!isOpenedByDeeplink) {
        startNextActivity()
        finish()
    }
}
```

---

## 4. DATABASE OPERATIONS

### 4.1 Room Database Schema

**File**: IdolDatabase.kt

```kotlin
@Database(
    entities = [IdolLocal::class, NotificationLocal::class],
    version = IdolRoomConstant.ROOM_VERSION
)
@TypeConverters(Converters::class)
abstract class IdolDatabase : RoomDatabase() {
    abstract fun idolDao(): IdolDao
    abstract fun notificationDao(): NotificationDao
}
```

**Entities**:
- **IdolLocal**: Cached idol data
- **NotificationLocal**: Local notifications

---

### 4.2 Idol Data Persistence

**UseCase Pattern**:

```kotlin
// Delete all and save new idols
class DeleteAllAndSaveIdolsUseCase

// Get idol by ID
class GetIdolByIdUseCase

// Upsert with timestamp
class UpsertIdolsWithTsUseCase

// Update heart and top3 fields
class UpdateHeartAndTop3UseCase

// Get viewable idols
class GetViewableIdolsUseCase

// Update anniversaries
class UpdateAnniversariesUseCase
```

---

## 5. SHARED PREFERENCES KEYS

```kotlin
// Account
PREFS_ACCOUNT = "account"
PREF_KEY__EMAIL = "email"
PREF_KEY__TOKEN = "token"
PREF_KEY__DOMAIN = "domain"

// UI Preferences
Const.KEY_DARKMODE = "dark_mode"
Const.PREF_ANIMATION_MODE = "animation_mode"
Const.MAIN_BOTTOM_TAB_CURRENT_INDEX = "main_bottom_tab_index"
Const.PREF_HAS_SHOWN_MY_FAV_TOAST = "has_shown_my_fav_toast"

// Update Flags
Const.PREF_ALL_IDOL_UPDATE = "all_idol_update_v2"
Const.PREF_DAILY_IDOL_UPDATE = "daily_idol_update_v2"
Const.PREF_OFFICIAL_CHANNEL_UPDATE = "official_channel_update"
Const.PREF_SHOULD_CALL_OFFICIAL_CHANNEL = "should_call_official_channel"

// Configurations
Const.PREF_SERVER_URL = "server_url"
Const.PREF_TV_TALK_ID = "tv_talk_id"
Const.PREF_DEFAULT_CATEGORY = "default_category"

// Settings
Const.TIME_ZONE = "time_zone"
Const.PREF_HEART_BOX_VIEWABLE = "heart_box_viewable"
Const.AWARD_RANKING = "award_ranking"
Const.IS_VIDEO_SOUND_ON = "is_video_sound_on"

// Lists & Data
Const.BAD_WORDS = "bad_words"
Const.LG_CODE = "lg_code"
Const.PREF_OFFICIAL_CHANNELS = "official_channels"
Const.PREF_NOTICE_LIST = "notice_list"
Const.PREF_EVENT_LIST = "event_list"
Const.PREF_FAMILY_APP_LIST = "family_app_list"
Const.PERF_UPLOAD_VIDEO_SPEC = "upload_video_spec"
Const.PREF_END_POPUP = "end_popup"
Const.PREF_HELP_INFO = "help_info"
Const.PREF_NEW_PICKS = "new_picks"
Const.AD_TYPE_LIST = "ad_type_list"
Const.AWARD_MODEL = "award_model"
Const.BOARD_TAGS = "board_tags"
Const.SELECTED_TAG_IDS = "selected_tag_ids"
Const.PREF_TYPE_LIST = "type_list"
Const.PREF_MOST_CHART_CODE = "most_chart_code"
Const.USER_BLOCK_LIST = "user_block_list"
Const.USER_BLOCK_FIRST = "user_block_first"
Const.PREF_SHOW_WARNING_PLATFORM_CHANGE_SUBSCRIPTION = "show_warning_platform_change_subscription"

// Advertising
Const.PROPERTY_AD_ID = "ad_id"
```

---

## 6. ERROR HANDLING & RETRY LOGIC

### 6.1 Network Error Handling

```kotlin
private fun handleError(result: BaseModel<*>) {
    viewModelScope.launch(Dispatchers.Main) {
        if(!result.success) {
            configLoadResult = false
            
            // Show toast once
            if (didShowToast.compareAndSet(false, true)) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                
                // Network connectivity error
                if(result.error is IOException) {
                    Toast.makeText(context, 
                        context.getString(R.string.desc_failed_to_connect_internet), 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

### 6.2 HTTP Status Code Handling

```kotlin
// 401 Unauthorized - Clear account and retry login
if(result.code == 401) {
    account?.clearAccount(context)
    _recreateActivity.value = Event(true)
    return@collectLatest
}

// 304 Not Modified - Use cached data (ETag based)
if (result.code == 304) {
    Logger.v("ApiLogger", "users/self 304 ${result.message}")
    // Continue with existing data
}

// 88888 - Server maintenance
if (response.optInt("gcode") == ERROR_88888 && response.optInt("mcode") == 1) {
    Util.showDefaultIdolDialogWithBtn1(
        this@StartupActivity,
        null,
        response.optString("msg"),
        R.drawable.img_maintenance
    ) { v ->
        finishAffinity()
    }
}

// 8000 - Account subscription issues
if (response.optInt("gcode") == ERROR_8000) {
    Util.showDefaultIdolDialogWithBtn1(
        this@StartupActivity,
        null,
        getString(R.string.subscription_account_holding),
        { openPlayStoreAccount() }
    )
}
```

---

## 7. PARALLEL EXECUTION & SYNCHRONIZATION

### 7.1 CountDownLatch Usage

```kotlin
// Subscription verification
val latch = CountDownLatch(subscriptions.size)

for (subscription in subscriptions) {
    // Async API call
    usersRepository.iabVerify(
        listener = { response ->
            // Process response
            latch.countDown()
        }
    )
}

latch.await()  // Wait for all verifications
```

### 7.2 Coroutine Async/Await

```kotlin
val tasks = listOf(
    async { getUpdateInfo(context) },
    async { getAdTypeList(context) },
    async { timeZoneUpdate(context) },
    async { getConfigSelf(context) },
    async { getMessageCoupon(context) },
    async { getUserSelf(context, account) },
    async { getUserStatus(account) }
).toMutableList()

tasks.awaitAll()  // Wait for all tasks
```

### 7.3 SupervisorScope for Award Data

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    deleteAllAwardsIdolUseCase().collectLatest {
        supervisorScope {
            charCodes.forEach { chartCode ->
                launch {
                    getAwardIdolUseCase(chartCode).collect { result ->
                        // Process idol data
                    }
                }
            }
        }
    }
}
```

---

## 8. DEPENDENCY INJECTION (Hilt)

```kotlin
@AndroidEntryPoint
class StartupActivity : BaseActivity()

@HiltViewModel
class StartupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val idolApiManager: IdolApiManager,
    private val getConfigStartupUseCase: GetConfigStartupUseCase,
    private val getUpdateInfoUseCase: GetUpdateInfoUseCase,
    private val getAdTypeListUseCase: GetAdTypeListUseCase,
    private val updateTimeZoneUseCase: UpdateTimeZoneUseCase,
    private val getConfigSelfUseCase: GetConfigSelfUseCase,
    // ... more dependencies
) : ViewModel()
```

**Injection Points in Activity**:

```kotlin
@Inject lateinit var usersRepository: UsersRepository
@Inject lateinit var miscRepository: MiscRepository
@Inject lateinit var accountManager: IdolAccountManager
@Inject lateinit var deleteAllAndSaveIdolsUseCase: DeleteAllAndSaveIdolsUseCase
@Inject lateinit var getIdolByIdUseCase: GetIdolByIdUseCase
@Inject lateinit var upsertIdolsWithTsUseCase: UpsertIdolsWithTsUseCase
@Inject lateinit var updateHeartAndTop3UseCase: UpdateHeartAndTop3UseCase
@Inject lateinit var favoritesRepository: FavoritesRepository
@Inject lateinit var idolsRepository: IdolsRepository
@Inject lateinit var idolApiManager: IdolApiManager
@Inject lateinit var getUserSelfUseCase: GetUserSelfUseCase
```

---

## 9. FLAVOR VARIANTS

The startup logic has flavor-specific variations:

### 9.1 BuildConfig Conditionals

```kotlin
// CELEB flavor only
if (BuildConfig.CELEB) {
    tasks.add(async { getTypeList(context) })
}

// CHINA flavor
if (BuildConfig.CHINA) {
    PushyUtil.registerDevice(this, null)
    ChinaUtil.initNativeX(this)
}

// ONESTORE flavor
if (!BuildConfig.ONESTORE) {
    checkSubscriptions()
}

if (BuildConfig.ONESTORE) {
    usersRepository.iabVerify(...)
} else {
    usersRepository.paymentsGoogleItem(...)
}
```

### 9.2 Flavor-Specific Paths

```
app/src/celeb/     - Celebrity version (Korean actors)
app/src/china/     - China region version
app/src/onestore/  - OneStore payment system
app/src/main/      - Shared core logic
```

---

## 10. MIGRATION PATH TO MODERN ARCHITECTURE

### 10.1 Key Transformations

1. **Thread → Coroutine**:
   - Remove StartupThread
   - Use viewModelScope.launch

2. **CountDownLatch → awaitAll()**:
   - Replace blocking waits with async/await

3. **LiveData → StateFlow/MutableStateFlow**:
   - Better composability and testing

4. **SharedPreferences → DataStore**:
   - Protocol buffers, type-safe, reactive

5. **Activity as presenter → Pure UI layer**:
   - Move all business logic to ViewModel

### 10.2 Recommended Structure

```
presentation/
  ├─ startup/
  │  ├─ StartupScreen.kt         (Pure composable/fragment)
  │  ├─ StartupViewModel.kt      (Business logic + state)
  │  └─ StartupContract.kt       (State & intent definitions)
  
domain/
  ├─ usecase/
  │  ├─ InitializeAppUseCase.kt
  │  ├─ SyncIdolDataUseCase.kt
  │  ├─ VerifySubscriptionUseCase.kt
  │  └─ VerifyIabUseCase.kt
  
data/
  ├─ repository/
  │  ├─ StartupRepository.kt
  │  ├─ IdolSyncRepository.kt
  │  └─ BillingRepository.kt
  
  ├─ api/
  │  ├─ ConfigsApi.kt
  │  ├─ UsersApi.kt
  │  └─ IdolsApi.kt
  
  ├─ local/
  │  └─ IdolDatabase.kt
```

---

## 11. API ENDPOINTS SUMMARY

| Endpoint | Method | Purpose | Critical |
|----------|--------|---------|----------|
| `/configs/startup/` | GET | App configuration | ✓ YES |
| `/configs/self/` | GET | User preferences | ✓ YES |
| `/update/` | GET | Idol data update flags | ✓ YES |
| `/users/self/` | GET | User profile (with ETag caching) | ✓ YES |
| `/users/status/` | GET | Tutorial status | NO |
| `/users/timezone_update/` | POST | Timezone sync | NO |
| `/idols/` | GET | Idol list (various filters) | ✓ YES |
| `/idol_supports/type_list/` | GET | Ad types | NO |
| `/messages/` | GET | Coupons/messages | NO |
| `/awards/current/` | GET | Award charts | NO |
| `/idols/?chart_code=` | GET | Award chart idols | NO |
| `/offerwalls/exodus_reward/` | POST | Offerwall reward | NO |
| `/configs/typelist/` | GET | Category types | NO (CELEB only) |
| `/blocks/` | GET | Blocked users | NO (first-time users) |
| `/users/iab_key/` | GET | IAB public key | ✓ YES |
| `/users/iab_verify/` | POST | IAB verification | ✓ YES |
| `/users/payments/google_item/` | POST | Google Play item verify | ✓ YES |
| `/users/payments/google_subscription_check/` | POST | Google subscription verify | ✓ YES |

---

## 12. TIMING & PERFORMANCE CONSIDERATIONS

### 12.1 Critical Path
1. Account check (fastest)
2. getConfigStartup() (prerequisite for others)
3. getConfigSelf() (prerequisite for award tab)
4. getUserSelf() (user data)
5. Idol data sync (dependent on update flags)

### 12.2 Parallel Operations
- getUpdateInfo, getAdTypeList, timeZoneUpdate, getUserStatus, getMessageCoupon can run in parallel

### 12.3 Timeouts
- Google Advertising ID: ~2 seconds
- API calls: Retrofit default + custom interceptors
- Total expected startup time: 3-8 seconds depending on network

---

## 13. KEY SECURITY CONSIDERATIONS

1. **VM Detection**: Prevent emulator fraud
2. **Public Key Decryption**: IAB key obfuscation with XOR
3. **ETag Caching**: 304 responses avoid re-downloading user data
4. **Signature Verification**: In-app purchase validation
5. **Account Clearing**: 401 responses trigger re-authentication
6. **Platform Checking**: Verify purchases on correct app

---

