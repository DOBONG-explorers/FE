package kr.ac.duksung.dobongzip.ui.threed

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.webkit.WebSettings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.commit
import com.google.android.material.tabs.TabLayout
import kr.ac.duksung.dobongzip.MainActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.databinding.ThreeDActivityBinding
import kr.ac.duksung.dobongzip.ui.review.PlaceReviewFragment
import java.util.Locale
import org.json.JSONObject

class ThreeDActivity : AppCompatActivity() {

    private val binding: ThreeDActivityBinding by lazy {
        ThreeDActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: ThreeDViewModel by viewModels()

    private val tabContentViews = mutableListOf<View>()

    private var currentContent: PlaceContent = PlaceContent()
    private var currentPlaceId: String? = null
    private var currentWebLatLng: Pair<Double, Double>? = null
    private var currentPlaceName: String? = null

    private var lastLogTime = 0L
    private val logThrottleMs = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupUi()

        val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME)
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN).takeIf { !it.isNaN() }
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN).takeIf { !it.isNaN() }

        if (placeId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.message_missing_place), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentPlaceId = placeId
        currentContent = PlaceContent(
            address = intent.getStringExtra(EXTRA_ADDRESS),
            description = intent.getStringExtra(EXTRA_DESCRIPTION),
            openingHours = intent.getStringArrayListExtra(EXTRA_OPENING_HOURS)?.filter { it.isNotBlank() },
            priceLevel = takeNullableIntExtra(EXTRA_PRICE_LEVEL),
            rating = intent.getDoubleExtra(EXTRA_RATING, Double.NaN).takeIf { !it.isNaN() },
            reviewCount = intent.getIntExtra(EXTRA_REVIEW_COUNT, -1).takeIf { it >= 0 },
            phone = intent.getStringExtra(EXTRA_PHONE),
            latitude = latitude,
            longitude = longitude
        )

        currentPlaceName = placeName
        binding.titleOverlay.text = placeName.orEmpty()
        loadWebContent(placeName, latitude, longitude)
        renderTabs(currentContent)

        observeViewModel(placeId)
        if (!placeId.startsWith("dummy_") && !placeId.startsWith("wondang_")) {
            viewModel.loadPlaceDetail(placeId)
        }
    }

    override fun onDestroy() {
        binding.webview3d.apply {
            (parent as? ViewGroup)?.removeView(this)
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            clearCache(true)
            destroy()
        }
        super.onDestroy()
    }

    private fun setupUi() {
        setupWebView()
        setupBottomNavigation()
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun observeViewModel(placeId: String) {
        viewModel.detail.observe(this) { detail ->
            currentContent = currentContent.merge(detail)
            val lat = currentContent.latitude
            val lng = currentContent.longitude
            if (lat != null && lng != null) {
                val current = currentWebLatLng
                if (current == null || current.first != lat || current.second != lng) {
                    loadWebContent(currentPlaceName, lat, lng)
                }
            }
            renderTabs(currentContent)
        }

        viewModel.liked.observe(this) { liked ->
            updateHeartState(liked ?: false)
        }

        viewModel.error.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.consumeError()
            }
        }

        binding.btnHeart.setOnClickListener {
            viewModel.toggleLike(placeId)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webview3d.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        with(binding.webview3d.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = false
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setGeolocationEnabled(true)
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            databaseEnabled = false
            mediaPlaybackRequiresUserGesture = false
            blockNetworkLoads = false
            blockNetworkImage = false
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
            loadsImagesAutomatically = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        binding.webview3d.setInitialScale(100)
        binding.webview3d.isHorizontalScrollBarEnabled = false
        binding.webview3d.isVerticalScrollBarEnabled = false
        binding.webview3d.overScrollMode = View.OVER_SCROLL_NEVER
        binding.webview3d.isNestedScrollingEnabled = false

        binding.webview3d.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }

            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val message = msg.message()
                    val now = System.currentTimeMillis()

                    val isImportant = message.contains("Error", ignoreCase = true) ||
                            message.contains("Failed", ignoreCase = true) ||
                            message.contains("모델", ignoreCase = true) ||
                            message.contains("model", ignoreCase = true) ||
                            message.contains("GLB", ignoreCase = true) ||
                            message.contains("404", ignoreCase = true) ||
                            message.contains("setPlaceInfo", ignoreCase = true) ||
                            message.contains("WebGL", ignoreCase = true)

                    if (isImportant && (now - lastLogTime > logThrottleMs)) {
                        val level = when (msg.messageLevel()) {
                            android.webkit.ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                            android.webkit.ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                            else -> Log.INFO
                        }
                        Log.println(level, "WebViewConsole", "${msg.sourceId()}:${msg.lineNumber()} $message")
                        lastLogTime = now
                    }
                }
                return true
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return false
            }
        }

        binding.webview3d.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun handleNetworkError(error: String) {
                if (error.contains("network error") || error.contains("TypeError")) {
                    return
                }
            }

            @android.webkit.JavascriptInterface
            fun onDownloadProgress(loaded: Long, total: Long, url: String) {
                Handler(Looper.getMainLooper()).post {
                    val percent = if (total > 0) (loaded * 100 / total).toInt() else 0
                    val loadedMB = String.format(Locale.KOREA, "%.1f", loaded / 1024.0 / 1024.0)
                    val totalMB = String.format(Locale.KOREA, "%.1f", total / 1024.0 / 1024.0)
                    if (url.contains(".glb") || url.contains(".gltf")) {
                        Log.d("DownloadProgress", "다운로드 진행: $percent% ($loadedMB MB / $totalMB MB)")
                    }
                }
            }

            @android.webkit.JavascriptInterface
            fun onModelLoadingProgress(progress: Int) {
                Handler(Looper.getMainLooper()).post {
                    val safeProgress = progress.coerceIn(0, 100)
                    binding.progressBarModel.progress = safeProgress
                    if (safeProgress >= 100) {
                        binding.progressBarModel.visibility = View.GONE
                    } else if (binding.progressBarModel.visibility != View.VISIBLE) {
                        binding.progressBarModel.visibility = View.VISIBLE
                    }
                }
            }
        }, "AndroidNetworkHandler")

        binding.webview3d.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.postDelayed({
                    val placeName = currentPlaceName ?: ""
                    val lat = currentContent.latitude ?: 0.0
                    val lng = currentContent.longitude ?: 0.0

                    val networkErrorHandlerJs = """
                        (function() {
                            var downloadCache = new Map();
                            var activeDownloads = new Map();
                            
                            function reportProgress(loaded, total, url) {
                                if (window.AndroidNetworkHandler && typeof window.AndroidNetworkHandler.onDownloadProgress === 'function') {
                                    try {
                                        window.AndroidNetworkHandler.onDownloadProgress(loaded, total, url);
                                    } catch(e) {}
                                }
                            }
                            
                            function downloadWithChunks(url, chunkSize) {
                                chunkSize = chunkSize || 10 * 1024 * 1024;
                                
                                return fetch(url, {
                                    method: 'HEAD',
                                    headers: {
                                        'Accept-Encoding': 'gzip, deflate, br',
                                        'Cache-Control': 'public, max-age=31536000'
                                    }
                                }).then(function(headResponse) {
                                    var totalSize = parseInt(headResponse.headers.get('content-length') || '0', 10);
                                    var acceptRanges = headResponse.headers.get('accept-ranges') === 'bytes';
                                    
                                    if (!acceptRanges || totalSize < chunkSize * 2) {
                                        return null;
                                    }
                                    
                                    var numChunks = Math.ceil(totalSize / chunkSize);
                                    var chunks = new Array(numChunks);
                                    var loadedChunks = 0;
                                    var totalLoaded = 0;
                                    
                                    function downloadChunk(start, end, index) {
                                        return fetch(url, {
                                            headers: {
                                                'Range': 'bytes=' + start + '-' + end,
                                                'Accept-Encoding': 'gzip, deflate, br',
                                                'Cache-Control': 'public, max-age=31536000'
                                            }
                                        }).then(function(response) {
                                            if (!response.ok && response.status !== 206) {
                                                throw new Error('Chunk download failed: ' + response.status);
                                            }
                                            return response.arrayBuffer();
                                        }).then(function(buffer) {
                                            chunks[index] = buffer;
                                            loadedChunks++;
                                            totalLoaded += buffer.byteLength;
                                            reportProgress(totalLoaded, totalSize, url);
                                            return { index: index, buffer: buffer };
                                        });
                                    }
                                    
                                    var promises = [];
                                    for (var i = 0; i < numChunks; i++) {
                                        var start = i * chunkSize;
                                        var end = Math.min(start + chunkSize - 1, totalSize - 1);
                                        promises.push(downloadChunk(start, end, i));
                                    }
                                    
                                    return Promise.all(promises).then(function(results) {
                                        if (results.length === numChunks) {
                                            results.sort(function(a, b) { return a.index - b.index; });
                                            var totalLength = results.reduce(function(sum, r) {
                                                return sum + r.buffer.byteLength;
                                            }, 0);
                                            var merged = new Uint8Array(totalLength);
                                            var offset = 0;
                                            for (var i = 0; i < results.length; i++) {
                                                merged.set(new Uint8Array(results[i].buffer), offset);
                                                offset += results[i].buffer.byteLength;
                                            }
                                            var blob = new Blob([merged]);
                                            downloadCache.set(url, blob);
                                            return new Response(blob, {
                                                status: 200,
                                                statusText: 'OK',
                                                headers: headResponse.headers
                                            });
                                        }
                                        return null;
                                    });
                                });
                            }
                            
                            if (window.fetch) {
                                var originalFetch = window.fetch;
                                window.fetch = function(url, options) {
                                    var urlString = typeof url === 'string' ? url : url.toString();
                                    
                                    if (downloadCache.has(urlString)) {
                                        var cachedBlob = downloadCache.get(urlString);
                                        return Promise.resolve(new Response(cachedBlob, {
                                            status: 200,
                                            statusText: 'OK'
                                        }));
                                    }
                                    
                                    if (activeDownloads.has(urlString)) {
                                        return activeDownloads.get(urlString);
                                    }
                                    
                                    if (!options) options = {};
                                    if (!options.headers) options.headers = {};
                                    
                                    if (urlString.match(/\\.(glb|gltf)$/i)) {
                                        options.headers['Cache-Control'] = 'public, max-age=31536000';
                                        options.headers['Accept-Encoding'] = 'gzip, deflate, br';
                                        
                                        var downloadPromise = downloadWithChunks(urlString, 10 * 1024 * 1024).then(function(chunkedResponse) {
                                            if (chunkedResponse) {
                                                activeDownloads.delete(urlString);
                                                return chunkedResponse;
                                            }
                                            
                                            return originalFetch.apply(this, arguments).then(function(response) {
                                                if (response.body && response.ok) {
                                                    var reader = response.body.getReader();
                                                    var contentLength = parseInt(response.headers.get('content-length') || '0', 10);
                                                    var loaded = 0;
                                                    var chunks = [];
                                                    var lastReportTime = Date.now();
                                                    
                                                    function pump() {
                                                        return reader.read().then(function(result) {
                                                            if (result.done) {
                                                                var blob = new Blob(chunks);
                                                                downloadCache.set(urlString, blob);
                                                                activeDownloads.delete(urlString);
                                                                var newResponse = new Response(blob, {
                                                                    status: response.status,
                                                                    statusText: response.statusText,
                                                                    headers: response.headers
                                                                });
                                                                return newResponse;
                                                            }
                                                            loaded += result.value.length;
                                                            chunks.push(result.value);
                                                            
                                                            var now = Date.now();
                                                            if (now - lastReportTime > 100) {
                                                                reportProgress(loaded, contentLength, urlString);
                                                                lastReportTime = now;
                                                            }
                                                            return pump();
                                                        });
                                                    }
                                                    return pump();
                                                }
                                                activeDownloads.delete(urlString);
                                                return response;
                                            });
                                        }).catch(function(error) {
                                            activeDownloads.delete(urlString);
                                            if (window.AndroidNetworkHandler && typeof window.AndroidNetworkHandler.handleNetworkError === 'function') {
                                                try {
                                                    window.AndroidNetworkHandler.handleNetworkError(error.toString());
                                                } catch(e) {}
                                            }
                                            throw error;
                                        });
                                        
                                        activeDownloads.set(urlString, downloadPromise);
                                        return downloadPromise;
                                    }
                                    
                                    return originalFetch.apply(this, arguments).catch(function(error) {
                                        if (window.AndroidNetworkHandler && typeof window.AndroidNetworkHandler.handleNetworkError === 'function') {
                                            try {
                                                window.AndroidNetworkHandler.handleNetworkError(error.toString());
                                            } catch(e) {}
                                        }
                                        throw error;
                                    });
                                };
                            }
                            if (window.XMLHttpRequest) {
                                var OriginalXHR = window.XMLHttpRequest;
                                window.XMLHttpRequest = function() {
                                    var xhr = new OriginalXHR();
                                    var originalOpen = xhr.open;
                                    var originalSend = xhr.send;
                                    var requestUrl = '';
                                    xhr.open = function(method, url) {
                                        requestUrl = url;
                                        originalOpen.apply(this, arguments);
                                    };
                                    xhr.send = function() {
                                        if (requestUrl.match(/\\.(glb|gltf)$/i)) {
                                            var lastReportTime = Date.now();
                                            xhr.addEventListener('progress', function(e) {
                                                if (e.lengthComputable) {
                                                    var now = Date.now();
                                                    if (now - lastReportTime > 100) {
                                                        reportProgress(e.loaded, e.total, requestUrl);
                                                        lastReportTime = now;
                                                    }
                                                }
                                            });
                                        }
                                        xhr.addEventListener('error', function() {
                                            if (window.AndroidNetworkHandler && typeof window.AndroidNetworkHandler.handleNetworkError === 'function') {
                                                try {
                                                    window.AndroidNetworkHandler.handleNetworkError('XMLHttpRequest error');
                                                } catch(e) {}
                                            }
                                        });
                                        return originalSend.apply(this, arguments);
                                    };
                                    return xhr;
                                };
                            }
                        })();
                    """.trimIndent()

                    val webglFixJs = """
                        (function() {
                            var canvas = document.querySelector('canvas');
                            if (canvas) {
                                var originalGetContext = canvas.getContext.bind(canvas);
                                canvas.getContext = function(contextType, attributes) {
                                    if (contextType === 'webgl' || contextType === 'experimental-webgl') {
                                        var webgl2Context = originalGetContext('webgl2', attributes);
                                        if (webgl2Context) {
                                            return webgl2Context;
                                        }
                                    }
                                    return originalGetContext(contextType, attributes);
                                };
                            }
                            if (window.THREE && window.THREE.WebGLRenderer) {
                                var originalWebGLRenderer = window.THREE.WebGLRenderer;
                                window.THREE.WebGLRenderer = function(parameters) {
                                    if (!parameters) parameters = {};
                                    if (parameters && parameters.canvas) {
                                        var canvas = parameters.canvas;
                                        var existingContext = canvas.getContext('webgl2');
                                        if (existingContext) {
                                            parameters.context = existingContext;
                                        }
                                    }
                                    if (!parameters.powerPreference) {
                                        parameters.powerPreference = 'high-performance';
                                    }
                                    if (!parameters.antialias) {
                                        parameters.antialias = false;
                                    }
                                    if (!parameters.stencil) {
                                        parameters.stencil = false;
                                    }
                                    if (!parameters.depth) {
                                        parameters.depth = true;
                                    }
                                    if (!parameters.premultipliedAlpha) {
                                        parameters.premultipliedAlpha = false;
                                    }
                                    if (!parameters.preserveDrawingBuffer) {
                                        parameters.preserveDrawingBuffer = false;
                                    }
                                    var renderer = new originalWebGLRenderer(parameters);
                                    if (renderer.setPixelRatio) {
                                        var pixelRatio = Math.min(window.devicePixelRatio || 1, 2);
                                        renderer.setPixelRatio(pixelRatio);
                                    }
                                    return renderer;
                                };
                            }
                        })();
                    """.trimIndent()

                    val loadingManagerJs = """
                        (function() {
                            function reportModelProgress(progress) {
                                if (window.AndroidNetworkHandler && typeof window.AndroidNetworkHandler.onModelLoadingProgress === 'function') {
                                    try {
                                        var percent = Math.round(progress * 100);
                                        window.AndroidNetworkHandler.onModelLoadingProgress(percent);
                                    } catch(e) {}
                                }
                            }
                            
                            function setupLoadingManager() {
                                if (window.THREE && window.THREE.LoadingManager) {
                                    var originalLoadingManager = window.THREE.LoadingManager;
                                    window.THREE.LoadingManager = function(onLoad, onProgress, onError) {
                                        var manager = new originalLoadingManager(
                                            onLoad,
                                            function(url, loaded, total) {
                                                if (total > 0) {
                                                    var progress = loaded / total;
                                                    reportModelProgress(progress);
                                                }
                                                if (onProgress) {
                                                    onProgress(url, loaded, total);
                                                }
                                            },
                                            onError
                                        );
                                        return manager;
                                    };
                                }
                                
                                if (window.THREE && window.THREE.FileLoader) {
                                    var originalFileLoader = window.THREE.FileLoader;
                                    window.THREE.FileLoader = function(manager) {
                                        var loader = new originalFileLoader(manager);
                                        var originalLoad = loader.load;
                                        loader.load = function(url, onLoad, onProgress, onError) {
                                            return originalLoad.call(this, url, onLoad, function(event) {
                                                if (event.lengthComputable && event.total > 0) {
                                                    var progress = event.loaded / event.total;
                                                    reportModelProgress(progress);
                                                }
                                                if (onProgress) {
                                                    onProgress(event);
                                                }
                                            }, onError);
                                        };
                                        return loader;
                                    };
                                }
                                
                                if (window.THREE && window.THREE.GLTFLoader) {
                                    var originalGLTFLoader = window.THREE.GLTFLoader;
                                    window.THREE.GLTFLoader = function(manager) {
                                        var loader = new originalGLTFLoader(manager);
                                        var originalLoad = loader.load;
                                        loader.load = function(url, onLoad, onProgress, onError) {
                                            reportModelProgress(0);
                                            return originalLoad.call(this, url, function(gltf) {
                                                reportModelProgress(1);
                                                if (onLoad) {
                                                    onLoad(gltf);
                                                }
                                            }, function(event) {
                                                if (event.lengthComputable && event.total > 0) {
                                                    var progress = event.loaded / event.total;
                                                    reportModelProgress(progress);
                                                }
                                                if (onProgress) {
                                                    onProgress(event);
                                                }
                                            }, onError);
                                        };
                                        return loader;
                                    };
                                }
                                
                                if (window.THREE && window.THREE.OBJLoader) {
                                    var originalOBJLoader = window.THREE.OBJLoader;
                                    window.THREE.OBJLoader = function(manager) {
                                        var loader = new originalOBJLoader(manager);
                                        var originalLoad = loader.load;
                                        loader.load = function(url, onLoad, onProgress, onError) {
                                            reportModelProgress(0);
                                            return originalLoad.call(this, url, function(object) {
                                                reportModelProgress(1);
                                                if (onLoad) {
                                                    onLoad(object);
                                                }
                                            }, function(event) {
                                                if (event.lengthComputable && event.total > 0) {
                                                    var progress = event.loaded / event.total;
                                                    reportModelProgress(progress);
                                                }
                                                if (onProgress) {
                                                    onProgress(event);
                                                }
                                            }, onError);
                                        };
                                        return loader;
                                    };
                                }
                            }
                            
                            if (window.THREE) {
                                setupLoadingManager();
                            } else {
                                var checkThree = setInterval(function() {
                                    if (window.THREE) {
                                        clearInterval(checkThree);
                                        setupLoadingManager();
                                    }
                                }, 100);
                                setTimeout(function() {
                                    clearInterval(checkThree);
                                }, 10000);
                            }
                        })();
                    """.trimIndent()

                    view?.post {
                        view?.evaluateJavascript(networkErrorHandlerJs, null)
                        view?.postDelayed({
                            view?.evaluateJavascript(webglFixJs, null)
                            view?.postDelayed({
                                view?.evaluateJavascript(loadingManagerJs, null)
                            }, 100)
                        }, 50)
                    }

                    val placeInfoJson = JSONObject().apply {
                        put("name", placeName)
                        put("lat", lat)
                        put("lng", lng)
                        put("address", currentContent.address ?: "")
                        put("description", currentContent.description ?: "")
                        put("phone", currentContent.phone ?: "")
                        put("rating", currentContent.rating ?: 0.0)
                        put("reviewCount", currentContent.reviewCount ?: 0)
                        put("priceLevel", currentContent.priceLevel ?: 0)
                    }.toString()

                    val escapedJson = placeInfoJson
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")

                    val placeInfoJs = """
                        (function() {
                            try {
                                var placeInfo = JSON.parse('${escapedJson}');
                                if (typeof window.setPlaceInfoFromObject === 'function') {
                                    window.setPlaceInfoFromObject(placeInfo);
                                } else if (typeof window.setPlaceInfo === 'function') {
                                    window.setPlaceInfo(placeInfo.name || '', 0, 0);
                                }
                            } catch(e) {}
                        })();
                    """.trimIndent()

                    view?.post {
                        view?.evaluateJavascript(placeInfoJs, null)
                    }

                    val modelStatusJs = """
                        (function() {
                            var checkCount = 0;
                            var lastCheckTime = 0;
                            var checkInterval = 15000;
                            var lastStatus = null;
                            var maxChecks = 10;
                            
                            function findThree() {
                                if (typeof window.THREE !== 'undefined') return window.THREE;
                                if (typeof THREE !== 'undefined') return THREE;
                                return null;
                            }
                            
                            function findScene() {
                                if (typeof window.scene !== 'undefined') return window.scene;
                                if (typeof scene !== 'undefined') return scene;
                                return null;
                            }
                            
                            function checkModelStatus() {
                                if (checkCount >= maxChecks) {
                                    return;
                                }
                                var now = Date.now();
                                if (now - lastCheckTime < checkInterval) {
                                    return;
                                }
                                lastCheckTime = now;
                                checkCount++;
                                
                                var THREE = findThree();
                                var scene = findScene();
                                
                                var status = {
                                    hasCanvas: !!document.querySelector('canvas'),
                                    hasThree: !!THREE,
                                    hasRenderer: !!(THREE && THREE.WebGLRenderer),
                                    hasScene: !!scene,
                                    hasModel: false,
                                    modelName: '',
                                    modelCount: 0
                                };
                                
                                if (status.hasScene) {
                                    try {
                                        var children = scene.children || [];
                                        status.modelCount = children.length;
                                        for (var i = 0; i < children.length; i++) {
                                            if (children[i].type === 'Group' || children[i].type === 'Mesh' || children[i].isMesh) {
                                                status.hasModel = true;
                                                status.modelName = children[i].name || children[i].type || 'Unknown';
                                                break;
                                            }
                                        }
                                    } catch(e) {}
                                }
                                
                                var statusStr = JSON.stringify(status);
                                if (statusStr !== JSON.stringify(lastStatus)) {
                                    console.log('[ModelStatus #' + checkCount + ']', statusStr);
                                    lastStatus = status;
                                }
                            }
                            
                            setTimeout(checkModelStatus, 10000);
                            var intervalId = setInterval(function() {
                                checkModelStatus();
                                if (checkCount >= maxChecks) {
                                    clearInterval(intervalId);
                                }
                            }, checkInterval);
                        })();
                    """.trimIndent()

                    view?.postDelayed({
                        view?.evaluateJavascript(modelStatusJs, null)
                    }, 5000)
                }, 100)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    view?.post {
                        Toast.makeText(
                            this@ThreeDActivity,
                            "웹페이지를 불러올 수 없습니다",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val url = request?.url?.toString() ?: ""
                if (url.contains(".glb") || url.contains(".gltf") || url.contains("model") ||
                    url.contains("assets") || url.contains("models")) {
                    return
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                return try {
                    super.shouldInterceptRequest(view, request)
                } catch (e: Exception) {
                    null
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                handler?.proceed()
            }
        }
    }

    private fun loadWebContent(placeName: String?, latitude: Double?, longitude: Double?) {
        val builder = Uri.parse(WEB_BASE_URL).buildUpon()
        if (!placeName.isNullOrBlank()) {
            builder.appendQueryParameter("place", placeName)
        }
        if (latitude != null && longitude != null) {
            builder.appendQueryParameter("lat", latitude.toString())
                .appendQueryParameter("lng", longitude.toString())
            currentWebLatLng = latitude to longitude
        } else {
            currentWebLatLng = null
        }
        builder.appendQueryParameter("ts", System.currentTimeMillis().toString())
        val url = builder.build().toString()
        binding.webview3d.loadUrl(url)
    }

    private fun renderTabs(content: PlaceContent) {
        val address = content.address
        val description = content.description
        val openingHours = content.openingHours?.takeIf { it.isNotEmpty() }
        val priceLevel = content.priceLevel
        val ratingInfo = content.rating?.let { rating ->
            val countText = content.reviewCount?.let { " (${it}건)" } ?: ""
            "평점 ${String.format(Locale.KOREA, "%.1f", rating)}$countText"
        }
        val phone = content.phone

        val commonTabs = listOf(
            TabSpec("장소 소개") {
                buildSection(
                    title = "장소 소개",
                    contents = buildList {
                        if (!address.isNullOrBlank()) add("주소: $address")
                        if (!phone.isNullOrBlank()) add("전화: $phone")
                        if (!description.isNullOrBlank()) add(description)
                        if (isEmpty()) add("장소 소개 정보가 없습니다.")
                    }
                )
            },
            TabSpec("이용 시간") {
                buildSection(
                    title = "이용 시간",
                    contents = openingHours?.takeIf { it.isNotEmpty() }
                        ?: listOf("이용 시간 정보가 없습니다.")
                )
            },
            TabSpec("이용 금액") {
                val priceText = priceLevel?.let { mapPriceLevel(it) }
                    ?: "이용 금액 정보가 없습니다."
                buildSection(
                    title = "이용 금액",
                    contents = listOf(priceText)
                )
            },
        )

        val reviewTab = currentPlaceId?.let { placeId ->
            TabSpec("리뷰") {
                createReviewTabContent(placeId)
            }
        } ?: TabSpec("리뷰") {
            buildSection(
                title = "리뷰",
                contents = listOf("리뷰 데이터를 불러올 수 없습니다.")
            )
        }

        val tabs = commonTabs + reviewTab

        binding.tabLayout.clearOnTabSelectedListeners()
        binding.tabLayout.removeAllTabs()
        binding.tabContentContainer.removeAllViews()
        tabContentViews.clear()

        tabs.forEachIndexed { index, tabSpec ->
            val tab = binding.tabLayout.newTab().setText(tabSpec.title)
            binding.tabLayout.addTab(tab, index == 0)

            val contentView = tabSpec.contentBuilder()
            contentView.isVisible = index == 0
            binding.tabContentContainer.addView(contentView)
            tabContentViews.add(contentView)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabVisibility(tab.position)
                val isReviewTab = tab.position == tabs.size - 1
                if (isReviewTab) {
                    binding.webviewContainer.visibility = View.GONE
                    val params = binding.tabLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.headerBar.id
                    binding.tabLayout.layoutParams = params
                } else {
                    binding.webviewContainer.visibility = View.VISIBLE
                    val params = binding.tabLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topToBottom = binding.webviewContainer.id
                    binding.tabLayout.layoutParams = params
                }
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(0, 0)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        updateTabVisibility(binding.tabLayout.selectedTabPosition.coerceAtLeast(0))
    }

    private fun updateTabVisibility(selectedIndex: Int) {
        tabContentViews.forEachIndexed { index, view ->
            view.isVisible = index == selectedIndex
        }
    }

    private fun updateHeartState(isLiked: Boolean) {
        binding.btnHeart.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
        )
        binding.btnHeart.contentDescription = if (isLiked) {
            getString(R.string.content_desc_unlike)
        } else {
            getString(R.string.content_desc_like)
        }
    }

    private fun buildSection(title: String, contents: List<String>): View {
        val context = this
        val sectionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 24.dpToPx(context))
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        sectionLayout.addView(titleView)

        contents.forEach { text ->
            val bodyView = TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                setLineSpacing(4.dpToPx(context).toFloat(), 1f)
                setPadding(0, 8.dpToPx(context), 0, 0)
            }
            sectionLayout.addView(bodyView)
        }

        return sectionLayout
    }

    private fun createReviewTabContent(placeId: String): View {
        val containerId = View.generateViewId()
        val frame = FrameLayout(this).apply {
            id = containerId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16.dpToPx(this@ThreeDActivity), 0, 0)
        }
        supportFragmentManager.findFragmentByTag(REVIEW_FRAGMENT_TAG)?.let { fragment ->
            supportFragmentManager.commit {
                remove(fragment)
            }
        }
        supportFragmentManager.commit {
            replace(
                containerId,
                PlaceReviewFragment.newInstance(placeId, currentPlaceName),
                REVIEW_FRAGMENT_TAG
            )
        }
        return frame
    }

    private fun mapPriceLevel(priceLevel: Int): String {
        return when (priceLevel) {
            0 -> "무료"
            1 -> "저렴"
            2 -> "보통"
            3 -> "조금 비쌈"
            4 -> "매우 비쌈"
            else -> "이용 금액 정보가 없습니다."
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun takeNullableIntExtra(key: String): Int? {
        return if (intent.hasExtra(key)) intent.getIntExtra(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE } else null
    }

    private fun setupBottomNavigation() {
        binding.navView.selectedItemId = R.id.navigation_home
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home,
                R.id.navigation_chat,
                R.id.navigation_notifications,
                R.id.navigation_mypage -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_TARGET_DESTINATION, item.itemId)
                    })
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private data class TabSpec(
        val title: String,
        val contentBuilder: () -> View
    )

    private data class PlaceContent(
        val address: String? = null,
        val description: String? = null,
        val openingHours: List<String>? = null,
        val priceLevel: Int? = null,
        val rating: Double? = null,
        val reviewCount: Int? = null,
        val phone: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    private fun PlaceContent.merge(detail: PlaceDetailDto?): PlaceContent {
        if (detail == null) return this
        return PlaceContent(
            address = detail.address?.takeIf { it.isNotBlank() } ?: this.address,
            description = detail.description?.takeIf { it.isNotBlank() } ?: this.description,
            openingHours = detail.openingHours?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: this.openingHours,
            priceLevel = detail.priceLevel ?: this.priceLevel,
            rating = detail.rating?.toDouble() ?: this.rating,
            reviewCount = detail.reviewCount ?: this.reviewCount,
            phone = detail.phone?.takeIf { it.isNotBlank() } ?: this.phone,
            latitude = detail.location?.latitude ?: this.latitude,
            longitude = detail.location?.longitude ?: this.longitude
        )
    }

    companion object {
        private const val WEB_BASE_URL = "https://dobongvillage-5f531.web.app"
        private const val REVIEW_FRAGMENT_TAG = "three_d_review_fragment"

        const val EXTRA_PLACE_ID = "extra_place_id"
        const val EXTRA_PLACE_NAME = "extra_place_name"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_OPENING_HOURS = "extra_opening_hours"
        const val EXTRA_PRICE_LEVEL = "extra_price_level"
        const val EXTRA_RATING = "extra_rating"
        const val EXTRA_REVIEW_COUNT = "extra_review_count"
        const val EXTRA_PHONE = "extra_phone"

        fun createIntent(
            context: Context,
            placeId: String,
            placeName: String,
            latitude: Double,
            longitude: Double,
            address: String? = null,
            description: String? = null,
            openingHours: ArrayList<String>? = null,
            priceLevel: Int? = null,
            rating: Double? = null,
            reviewCount: Int? = null,
            phone: String? = null
        ): Intent {
            return Intent(context, ThreeDActivity::class.java).apply {
                putExtra(EXTRA_PLACE_ID, placeId)
                putExtra(EXTRA_PLACE_NAME, placeName)
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
                address?.let { putExtra(EXTRA_ADDRESS, it) }
                description?.let { putExtra(EXTRA_DESCRIPTION, it) }
                openingHours?.let { putStringArrayListExtra(EXTRA_OPENING_HOURS, it) }
                priceLevel?.let { putExtra(EXTRA_PRICE_LEVEL, it) }
                rating?.let { putExtra(EXTRA_RATING, it) }
                reviewCount?.let { putExtra(EXTRA_REVIEW_COUNT, it) }
                phone?.let { putExtra(EXTRA_PHONE, it) }
            }
        }
    }
}

