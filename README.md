# Store Apps - MVVM Clean Architecture (under development)

- Modularization
- UseCase
- Retrofit
- Moshi
- Dagger Hilt Dependencies Injection
- Kotlin flow (with stateFlow experimental)
- Paging 3
- Navigation Component
- Data Store Preferences
- Room
- Work Manager
- Download Manager
- ViewBinding (with helper by [yogacp](https://github.com/yogacp/android-viewbinding))

## Table of Contents
- [Architecture](#architecture)
    - [Stream Data Flow](#stream-data-flow)
    - [Modularization](#modularization)
    - [UseCase](#usecase)
        - [ResultState](#resultstate)
        - [Interactor](#interactor)
    - [Networking](#networking)
        - [Moshi](#moshi)
    - [Dependencies Injection](#dependencies-injection)
        - [Koin](#koin)
        - [Dagger Hilt](#dagger-hilt)
        - [Handle dependencies across module](#handle-dependencies-across-module)
    - [ViewModel](#viewmodel)
    - [Paging Library 3](#paging-library-3)
        - [DataSource](#data-source)
        - [Adapter](#adapter)
        - [LoadState Listener](#loadstate-listener)

---
|Home|Detail|Detail downloading|Download monitor|
|--|--|--|--|
|![](images/home.png?raw=true)|![](images/detail.png?raw=true)|![](images/detail_download.png?raw=true)|![](images/download_monitor.png?raw=true)|

|Download manager|Auto installer|Options auto installer|App updated|
|--|--|--|--|
|![](images/download_manager.png?raw=true)|![](images/installer.png?raw=true)|![](images/options_installer.png?raw=true)|![](images/updated.png?raw=true)|

---

## Architecture
Projek ini berusaha menerapkan Clean Arch yang baik demi pemeliharaan (maintenance) yang baik pula. Penerapan ini juga berusaha mengikuti prinsip-prinsip Clean Arch yang baik yakni SOLID Principles, (baca di sini https://en.wikipedia.org/wiki/SOLID). Untuk lebih jelas mengenai Android Clean Architecture, silahkan baca tulisan om Yoga [Clean Architecture Android](https://medium.com/style-theory-engineering/android-clean-architecture-using-kotlin-48306644ada7).

### Stream Data Flow
![](images/stream_data_flow.png?raw=true)

- **Network**: Data berasal dari API Aptoide dan dieksekusi pada network threading menggunakan Retrofit.
- **Repository** : Route retrofit berasal dari api service yang dipanggil oleh repository dengan fungsi interactor flow yang menghasilkan response yang telah wrap oleh class result.
- **Domain** : Result yang berisi state, loading, success atau error di convert ke dalam DTO yang bertindak sebagai data object yang akan diparsing ke dalam view. Result yang berisi state dari data object tersebut menjadi flow variable.
- **ViewModel** : Dalam viewmodel, flow variable tersebut di convert menjadi LiveData dengan ViewModelScope agar bisa dibinding oleh lifecycle activity atau fragment untuk dapat diteruskan menuju view.
- **View** : LiveData yang berisi result di ekstrak dalam state idle, loading, success dan error. Dimana state success akan menghasilkan data yang dapat diparsing dalam view, sementara error akan menghasilkan throwable.

---

### Modularization
Dalam proyek ini, saya memisahkan fitur dan layer secara modular sehingga mudah untuk menjaga kode dan tanggungjawab setiap class pada layer masing-masing dan ketergantungannya.

tree:
```
root
--- app
--- data
--- feature
------ detail
------ home
------ listing
--- libraries
------ abstraction
------ network
```

#### Layer?
Layer yang dimaksud dalam prinsip Clean Arch adalah lapisan-lapisan yang mewakili fungsionalitas dari class-class yang membangun sebuah fitur. Untuk gampang nya bisa lihat gambar berikut.

![](https://miro.medium.com/max/942/1*Jve_0_GCxLEiYzdc2QKogQ.jpeg)

Lingkaran tersebut berisi macam-macam lapisan atau layer. Layer-layer itulah yang membangun sebuah fitur dengan scope tugas masing-masing.

- **Presentation** : Ini adalah layer UI, tempat user berinteraksi, akhir dari semua aliran data (stream data). Activity atau fragment yang berisi view-view ada dalam layer ini
- **UseCase** : Layer ini berisi fungsi-fungsi tindakan user yang berasal dari view, inti dari bisnis logic berawal dari sini
- **Data** : Berisi semua sumber data. Stream data berawal dari sini kemudian diteruskan oleh bisnis logic class.
- **Domain** : Berisi logika bisnis dari sebuah fitur, biasa nya layer ini juga berisi UseCase class.
- **External** : Layer diluar bisnis logic dan view, biasa nya berisi abstraction dan helper.

Khusus untuk layer Data, module dipisah, isi nya berupa repository-repository dan class-class yang dibutuhkan pada module lain. Untuk module lain, dibuat perfitur, masing-masing module menghandle fiturnya masing-masing dimulai dengan fungsi yang disediakan repository pada module data, dengan begitu responsibility akan lebih terjaga.

### UseCase
UseCase dalam arsitektur ini menjadi jembatan bagi view dan data, satu level dengan Domain layer. Berisi fungsi-fungsi yang dapat dieksekusi atau diteruskan ke Data layer dan bergantung pada user input Presentasion layer. Dalam project ini, UseCase menghasilkan data yang dibungkus oleh `ResultState`.

Seperti dibahas di awal, layer ini berfungsi untuk menyimpan function-function yang terkait pengambilan data dari layer Data. Dalam project ini, proses tersebut menggunakan sebuah fungsi yang menghasilkan (`return`) `Flow<ResultState>`, fungsi ini dapat disebut dengan interactor.

#### ResultState
`ResultState` merupakan class "bungkusan" yang berfungsi membawa data dari hulu menuju hilir, dari layer Data sampai ke View. Class ini menggunakan fungsi `sealed` dari Kotlin yang berisi empat objek, `Loading`, `Idle`, `Success` dan `Error`.

```kotlin
sealed class ResultState<T: Any>(val payload: T? = null, val throwable: Throwable? = null, val message: String? = null) {
    class Loading<T: Any> : ResultState<T>()
    class Idle<T: Any>: ResultState<T>()
    data class Success<T: Any>(val data: T) : ResultState<T>(payload = data)
    data class Error<T: Any>(val th: Throwable) : ResultState<T>(throwable = th)
}
```

Yang merupakan data class turunan hanya state `Success` dan `Error` karena dua objek tersebut membawa data masing-masing, sementara `Loading` dan `Idle` hanya object tanpa membawa data. Dengan begitu, ketika ekstraksi pada view, data atau `payload` hanya dapat diakses pada state `success`, pun begitu pada `throwable` hanya dapat diakses pada `state` error.

```kotlin
viewModel.getRandomApps()
viewModel.randomList.observe(viewLifecycleOwner, Observer { state ->
    when (state) {
        is ResultState.Idle -> {
            // idle
        }
        is ResultState.Loading -> {
            // loading
        }
        is ResultState.Success -> {
            // success, get data
            val data = state.data
        }
        is ResultState.Error -> {
            // error, get throwable
            val throwable = state.throwable
        }
    }
})
```

#### Interactor
Interactor merupakan fungsi yang bertugas mengambil data dan menghasilkan class `Flow<ResultState>`. Digunakan untuk berinteraksi dengan suspend function yang mengambil data atau response dari repository atau dapat juga langsung dari route services. Baca mengenai `Flow` di sini https://kotlinlang.org/docs/reference/coroutines/flow.html

```kotlin
suspend fun <T: Any> fetch(call: suspend () -> T): Flow<ResultState<T>> = flow {
    emit(ResultState.Loading<T>())
    try {
        emit(ResultState.Success(data = call.invoke()))
    } catch (e: Throwable) {
        emit(ResultState.Error<T>(th = e))
    }
}
```

Dapat diperhatikan, fungsi interaktor ini menghasilkan `ResultState.Loading`, `ResultState.Success` dan `ResultState.Error`, lantas dimana `ResultState.Idle` (kondisi diam gak ngapa2in)?

`ResultState.Idle` didefiniskan diluar interactor, yakni pada nilai default dari variable sebuah `MutableStateFlow<ResultState>` pada class `UseCase`. State ini menandakan bahwa belum terjadi apa-apa, baik itu state loading, sukses ataupun error.

```kotlin
val resultData: MutableStateFlow<ResultState<AppsView>> = MutableStateFlow(ResultState.Idle())
```

Value `resultData` diperbarui dengan nilai yang dihasilkan pada interactor. Variable `resultData` ini lah yang akan diproses ke layer selanjutnya.

```kotlin
class HomeUseCase(private val appsRepository: AppsRepository) {

    // variable result dengan default value ResultState.Idle
    val randomList: MutableStateFlow<List<AppsView>> = MutableStateFlow(ResultState.Idle())

    // suspend function
    suspend fun getRandomApps() {
    
        // interactor
        fetch {
            // function dalam interactor
            val response = appsRepository.getTopApps()
            response.datalist?.list?.map { app ->

                // convert to object view (lihat gambar data flow di atas)                 
                app.toAppsView()
            } ?: emptyList()
        }.collect {
            
            // set value dari variable result
            randomList.value = it
        }
    }
}
```

### Networking
Networking menggunakan Retrofit, Coroutines dan Moshi adapter. Alih-alih menggunakan RxJava, Coroutine terlihat lebih *clean* dan simple, bukan berarti RxJava tidak bagus, tapi ini cuman pilihan. Tidak begitu dengan Moshi, saya berhenti menggunakan Gson dan beralih dengan Moshi dalam project-project riset seperti ini karena peforma dan kompabilitasnya dirasa lebih baik.

#### Moshi
Moshi merupakan JSON Library untuk android dan java yang dikembangkan oleh Square, pengembang yang sama untuk Retrofit. Saya sudah lama menggunakan Gson, tapi sepertinya saya harus mempertimbangkan Moshi yang akan jadi pilihan utama kedepan. Setelah baca beberapa artikel, sedikit catatan untuk itu.

- Gson bukan library yang modern, tidak ditulis dengan kotlin, sementara Moshi lebih ramah kotlin. Yang membuat ramah adalah sebagian kode ditulis dengan kotlin.
- Moshi memiliki ukuran yang lebih kecil dibanding Gson
- Moshi dikembangkan oleh developer yang sama dengan Retrofit. Hal ini memastikan update Retrofit kedepan akan kompatible dengan Moshi.

Meski begitu, sulit membuat function converter dari model ke string Json secara generik. Tidak seperti Gson yang hanya butuh type class, Moshi membutuhkan adapter pada tiap class generik dan memerlukan Buffer Reader UTF 8 untuk generate pretty nya. Lihat [JsonBeautifier.kt](libraries/network/src/main/java/com/utsman/network/utils/JsonBeautifier.kt)

### Dependencies Injection
Dependencies Injection (untuk selanjutnya disebut DI) merupakan library wajib bagi saya untuk mengerjakan projek-projek android. Setidaknya ada tiga poin utama yang di kemukakan oleh Google mengenai kelebihan menerapkan DI dalam arsitektur android.

- Penggunaan kembali kode
- Kemudahan dalam pemfaktoran ulang
- Kemudahan dalam pengujian

Baca selebihnya pada dokumentasi official Google di sini https://developer.android.com/training/dependency-injection?hl=id

#### Koin
Koin adalah salah satu library dependencies yang populer dikalangan android developer. Selain yang ramah kotlin, Koin juga mudah dalam penggunaan dan implementasi nya yang sederhana membuat orang yang sedang belajar DI jadi cepat mengerti.

Pada starting project ini, saya menggunakan Koin karena tidak terlalu memikirkan bagaimana peforma library DI bekerja dan fokus pada fitur juga komponen arch yang lain. Untuk melihat kemudahan menggunakan Koin dapat dibaca dokumentasi offical Koin, https://start.insert-koin.io/#/quickstart/android. Kemudian saya beralih pada Hilt, library DI offical Google.

#### Dagger Hilt
Hilt sama fungsinya seperti Koin, sama-sama library DI. Hanya Hilt merupakan library yang dikembangkan sendiri oleh Google dengan memanfaatkan Dagger sebagai basis kode. Library Dagger yang sudah cukup lama dan mapan sebagai library DI, dirasa cukup sulit penerapannya apalagi dimengerti, sehingga muncul banyak library alternatif lain seperti Koin diatas. Namun sekarang, Google telah mengembangkan Hilt yang mudah di implementasikan dan dimengerti tanpa menghilangkan komponen-komponen yang ada pada Dagger.

Dalam kode ini anda dapat menemukan banyak anotasi dan provide Hilt secara singleton. Saya hanya menggunakan singleton dan `ApplicationComponent` pada keseluhuran karena ketergantungan dependencies tidak sampai pada layer view. Jadi tiap-tiap dependencies dapat diakses pada semua scope project aplikasi tanpa ada batasan.

#### Handle dependencies across module
Karena semua dependencies di install pada module `ApplicationComponent`, maka diperlukan teknik mengambil class dependencies yang telah di provide, terutama pada class-class yang tidak memiliki `EntryPoint` seperti Activity, Service dan lain-lain (https://developer.android.com/training/dependency-injection/hilt-android?hl=id#kotlin).

Untuk keperluan tersebut, perlu dibuat variable global, yang dapat diakses oleh `MainApplication` sebagai scope utama project aplikasi. Bagusnya, pada kotlin, kita dapat mendeklarasikan variable global tanpa class, hanya ditulis pada file kotlin (*.kt). Tahap selanjutnya adalah menetapkan value dari masing-masing global variable di `MainApplication` dengan mengambil dependencies yang telah di inject.

Dalam membuat global variable tersebut, saya menggunakan `MutableStateFlow` sebagai bungkusannya. Sehingga variable tersebut dapat ditetapkan valuenya berdasarkan dependencies yang telah di provide. Skemanya adalah

Module B. Menetapkan global variable dengan nullable
```kotlin
// lateinit.kt module b
val _someVarState: MutableStateFlow<SomeVar?> = MutableStateFlow(null)
```

Module A. Menetapkan value variable tersebut dengan nilai yang sudah di provide
```kotlin
// MainApplication.kt module a
import module_b._someVarState

@HiltAndroidApp
class MainApplication : Application() {
        
        @Inject
        lateinit var someVar: SomeVar
        
        override fun onCreate() {
            super.onCreate()
            _someVarState.value = someVar
        }
}
```

Lantas, tinggal buat function helper agar terlihat lebih bagus yang dapat digunakan tiap-tiap class yang membutuhkan.

```kotlin
inline fun <reified T: Any?>getValueOf(instanceState: MutableStateFlow<T?>): T {
    val immutable: StateFlow<T?> = instanceState
    val className = T::class.simpleName
    return try {
        immutable.value!!
    } catch (e: ExceptionInInitializerError) {
        loge("Module not yet initialize. Check module of class : `$className`")
        throw e
    }
}

inline fun <reified T>getValueSafeOf(instanceState: MutableStateFlow<T>): T {
    val immutable: StateFlow<T> = instanceState
    return immutable.value
}
```

Ada dua function yang dibuat, `getValueOf()` menghasilkan nilai yang tidak null, karena global variable tersebut bersifat nullable, maka ditambahkan `not-null` assertion, cara ini dapat menyebabkan NPE jika value gagal di tetapkan pada `MainApplication`. Sementara `getValueSafeOf()` mengambil nilai yang nullable. Sehingga pemanggilan dependencies nya dapat dilakukan pada class seperti helper, WorkManager, interface dll.

```kotlin
class DownloadWorkManager(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    private val someVarState = getValueSafeOf(_someVarState)

    ...
}
```

### ViewModel
ViewModel berfungsi meneruskan dan menyimpan state variable (jika diperlukan) dari UseCase. Stream data (aliran data) berada disini setelah melewati UseCase. Di ViewModel juga ResultState yang bertipe `Flow` dikonversi menjadi `livedata` agar dapat dibinding pada lifecycle sebuah activity/fragment.

```kotlin
class SomeViewModel(private val useCase: SomeUseCase) : ViewModel() {

    val resultLiveData 
            get() = useCase.someResult.asLiveData(viewModelScope.coroutineContext)
}
```

### Paging Library 3
Paging library merupakan komponen dari Jetpack yang dapat menghandle banyak data pada recyclerview. Meski masih versi alpa, paging 3 cukup mapan untuk digunakan pada production, saya tidak menemui bug yang fatal selama riset Paging 3. Kelebihan yang signifikan dibanding paging 2 terletak pada `DataSource`, viewModel handling dan adapter. 

#### Data Source
Pada data source paging 2, developer perlu menentapkan 3 function, `loadInitial`, `loadAfter` dan `loadBefore`. Pada `loadInital` dan `loadAfter`, dapat terjadi redundant karena code implementasi nya sering kali sama, hanya param key yang bertindak sebagai "page" selanjutnya. Sementara pada Paging 3 hanya membutuhkan implementasi pada `load` function.
Dah gitu, data source pada paging 3 berdiri diatas coroutines functions, lihat [AppsPagingSource](data/src/main/java/com/utsman/data/source/AppsPagingSource.kt). Hal ini membuat developer lebih mudah implementasi async code.

#### Adapter
Pada adapter paging 2, mirip seperti `RecyclerView.Adapter` biasa, yang membedakan hanya tipe data (`PagedList` dan `List`) dan beberapa function seperti `submit`, `getItem`. Sementara pada Paging 3, ditambahkan listener `LoadState` juga footer atau header yang dapat di attach pada adapter. Ini membuat developer tidak perlu lagi membuat listener dan membuat network state dengan mutliple view type.

#### LoadState Listener
Ini adalah fitur yang ditambahkan pada paging 3, memungkinkan developer melihat state pada aliran data. Pada function `addLoadStateListener` dari adapter, developer bisa kontrol view saat data sedang `loading`, `notloading` dan `error` dengan lima kondisi, yakni:

- *source*: `LoadState` yang asli dari `DataSource`
- *refresh*: `LoadState` yang sedang memuat data baru pada `DataSource`
- *prepend*: `LoadState` awal dan belum memuat apa-apa dari aliran data pada `DataSource`
- *append*: `LoadState` akhir dari aliran data pada `DataSource`
- *mediator*: `LoadState` yang terdapat pada `RemoteMediator` jika `RemoteMediator` dipasang

Lihat dokumentasi [CombinedLoadStates](https://developer.android.com/reference/kotlin/androidx/paging/CombinedLoadStates)

Berdasarkan state-state tersebut, developer dapat memasang ui loading atau not-loading pada activity/fragment sebelum item dimuat oleh adapter.

```kotlin
pagingListAdapter.addLoadStateListener { combinedLoadStates ->

    val refreshState = combinedLoadStates.refresh // mengambil kondisi refresh load state
    progressCircular.isVisible = refreshState is LoadState.Loading // menampikan progress bar jika refresh state sedang loading
}
``` 

Paging 3 ini juga mempunyai state adapter dengan viewholder yang berbeda pada adapter utama. Adapter tersebut di pasang sejajar dengan adapter utama, fungsi ui nya dapat menampilkan status loading maupun error pada data yang sejajar dengan item utama yang telah di load sebelumnya. Jika pada paging 2 developer harus membuat network viewholder dengan teknik multiple viewtype dengan satu viewholder untuk menampilkan ui loading atau error, pada Paging 3 tidak perlu melakukan itu.

```kotlin
class PagingStateAdapter : LoadStateAdapter<PagingStateAdapter.PagingStateViewHolder>() {

    // viewholder single line
    class PagingStateViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onBindViewHolder(holder: PagingStateViewHolder, loadState: LoadState) {
        val binding = ItemListLoaderBinding.bind(holder.itemView)
        binding.run {
    
            // jika state loading menampilkan progress bar
            progressCircular.isVisible = loadState is LoadState.Loading

            // jika state error menampilkan textview dan button
            btnRetry.isVisible = loadState is LoadState.Error
            txtMsg.isVisible = loadState is LoadState.Error

            ....
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): PagingStateViewHolder {
        val view = parent.inflate(R.layout.item_list_loader)
        return PagingStateViewHolder(view)
    }
}
```
Lihat [PagingStateAdapter](libraries/abstraction/src/main/java/com/utsman/abstraction/base/PagingStateAdapter.kt)

Kemudian, state adapter tersebut dipasang dengan adapter utama

```kotlin
binding.recyclerView.run {

    val pagingListAdapter = PagingListAdapter() // adapter utama
    val pagingStateAdapter = PagingStateAdapter() // adapter state

    layoutManager = gridLayout
    adapter = pagingListAdapter.withLoadStateFooter(pagingStateAdapter) // pasang state adapter pada footer dari adapter utama
}
```

---
