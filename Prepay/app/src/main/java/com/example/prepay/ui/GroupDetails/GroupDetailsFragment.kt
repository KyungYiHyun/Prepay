package com.example.prepay.ui.GroupDetails

import android.Manifest
import android.app.AlertDialog
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prepay.BaseFragment
import com.example.prepay.CommonUtils
import com.example.prepay.PermissionChecker
import com.example.prepay.R
import com.example.prepay.RetrofitUtil
import com.example.prepay.SharedPreferencesUtil
import com.example.prepay.data.model.dto.RestaurantData
import com.example.prepay.data.response.BanUserReq
import com.example.prepay.data.response.MoneyChangeReq
import com.example.prepay.data.response.PrivilegeUserReq
import com.example.prepay.data.response.TeamIdReq
import com.example.prepay.data.response.TeamIdStoreRes
import com.example.prepay.data.response.TeamUserRes
import com.example.prepay.databinding.DialogAuthoritySettingBinding
import com.example.prepay.databinding.DialogGroupExitBinding
import com.example.prepay.databinding.DialogGroupResignBinding
import com.example.prepay.databinding.DialogInviteCodeBinding
import com.example.prepay.databinding.DialogMoneyChangeBinding
import com.example.prepay.databinding.FragmentGroupDetailsBinding
import com.example.prepay.ui.MainActivity
import com.example.prepay.ui.MainActivityViewModel
import com.example.prepay.ui.RestaurantDetails.RestaurantDetailsViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

private const val TAG = "GroupDetailsFragment_싸피"
class GroupDetailsFragment: BaseFragment<FragmentGroupDetailsBinding>(
    FragmentGroupDetailsBinding::bind,
    R.layout.fragment_group_details
), RestaurantAdapter.OnRestaurantClickListener, OnTeamUserActionListener{
    private lateinit var mainActivity: MainActivity
    private lateinit var restaurantAdapter: RestaurantAdapter
    private lateinit var teamUserAdapter: TeamUserAdapter
    private lateinit var restaurantList: List<TeamIdStoreRes>
    private lateinit var teamTeamUserResList: List<TeamUserRes>
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    //activityViewModel
    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private val viewModel: GroupDetailsFragmentViewModel by viewModels()
    private val restaurantDetailsViewModel : RestaurantDetailsViewModel by viewModels()

    private var inviteCode = "0"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity= context as MainActivity
        Log.d(TAG, activityViewModel.teamId.value.toString())
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_toolbar, menu) // 메뉴 추가
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ic_menu -> {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END) // 👉 열려 있으면 닫기
                } else {
                    binding.drawerLayout.openDrawer(GravityCompat.END)  // 👉 닫혀 있으면 열기
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        mainActivity.hideBottomNav(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEvent()
        initAdapter()
        initViewModel()
        initDrawerLayout()
        initialView()
    }

    override fun onPause() {
        super.onPause()
        mainActivity.hideBottomNav(false)
    }

    private fun initAdapter(){
        teamTeamUserResList = emptyList()
        restaurantList = emptyList()
        restaurantAdapter = RestaurantAdapter(restaurantList,this)
        teamUserAdapter = TeamUserAdapter(teamTeamUserResList,this, true)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = restaurantAdapter
        binding.rvMemberList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMemberList.adapter = teamUserAdapter
    }

    private fun initViewModel(){
        viewModel.teamUserListInfo.observe(viewLifecycleOwner){it->
            teamUserAdapter.teamUserResList = it
            teamUserAdapter.notifyDataSetChanged()
        }
        viewModel.userposition.observe(viewLifecycleOwner){it->
            teamUserAdapter.userposition = it
            teamUserAdapter.notifyDataSetChanged()
        }
        viewModel.getMyTeamRestaurantList(SharedPreferencesUtil.getAccessToken()!!,activityViewModel.teamId.value!!)
        viewModel.getMyTeamUserList(SharedPreferencesUtil.getAccessToken()!!,activityViewModel.teamId.value!!);

        viewModel.moneyValue.observe(viewLifecycleOwner) { it->
            Log.d(TAG,"가격변동"+it.toString())
            //binding.usePossiblePriceTxt.text = it.toString()
            changeMoneyView()
        }
    }

    private fun initDrawerLayout(){
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
    }


    private fun initEvent() {
        binding.diningTogetherQrBtn.setOnClickListener {
            lifecycleScope.launch {
                runCatching {
                    RetrofitUtil.qrService.qrTeamCreate(SharedPreferencesUtil.getAccessToken()!!,activityViewModel.teamId.value!!.toInt())
                }.onSuccess {
                    Log.d(TAG,it.message)
                    showQRDialog(it.message)
                }.onFailure {
                    mainActivity.showToast("qr불러오기가 실패했습니다")
                }
            }
        }

        binding.groupInviteBtn.setOnClickListener {
            showInviteCodeInputDialog()
        }
        binding.groupExitBtn.setOnClickListener {
            showGroupExitDialog()
        }
        binding.addRestaurant.setOnClickListener {
            addRestaurantClick()
        }

        binding.moneyChangeBtn.setOnClickListener {
            showMoneyChangeDialog()
        }
        binding.qrBtn.setOnClickListener {
            lifecycleScope.launch {
                runCatching {
                    RetrofitUtil.qrService.qrPrivateCreate(SharedPreferencesUtil.getAccessToken()!!,activityViewModel.teamId.value!!.toInt())
                }.onSuccess {
                    Log.d(TAG,it.message)
                    showQRDialog(it.message+":"+SharedPreferencesUtil.getAccessToken()!!+":"+activityViewModel.teamId.value.toString())
                }.onFailure {
                    mainActivity.showToast("qr불러오기가 실패했습니다")
                }
            }
            //mainActivity.broadcast("hello","hello")
        }
    }

    fun changeMoneyView(){
        lifecycleScope.launch {
            kotlin.runCatching {
                RetrofitUtil.teamService.getTeamDetails(SharedPreferencesUtil.getAccessToken()!!, activityViewModel.teamId.value!!)
            }.onSuccess {
                Log.d(TAG,"얼마 사용"+it.dailyPriceLimit+" "+it.usedAmount)
                binding.usePossiblePriceTxt.text = CommonUtils.makeComma(viewModel.moneyValue.value!!.toInt() - it.usedAmount)
                viewModel.updatePosition(it.position)
                inviteCode = (it.teamPassword ?: "초대코드없음").toString()
            }.onFailure {
                Log.d(TAG, "서버 데이터 가져오기 실패")
            }
        }
    }



    fun initialView(){
        lifecycleScope.launch{
            kotlin.runCatching {
                RetrofitUtil.teamService.getTeamDetails(SharedPreferencesUtil.getAccessToken()!!,activityViewModel.teamId.value!!)
            }.onSuccess {
                binding.usePossiblePriceTxt.text = CommonUtils.makeComma(it.dailyPriceLimit-it.usedAmount)
                viewModel.updatePosition(it.position)
                inviteCode = (it.teamPassword ?: "초대코드없음").toString()
            }.onFailure {
                Log.d(TAG,"실패하였습니다")
            }
        }
    }

    //클릭 이벤트 등 집합
    private fun addRestaurantClick() {
        bringStoreId()
    }

    private fun bringStoreId() {
        mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.ADD_RESTAURANT_FRAGMENT)
    }

    override fun onRestaurantClick(storeName : String, teamIdStoreResId: Int) {
        Log.d(TAG, "teamIdStoreResId: $teamIdStoreResId")
        activityViewModel.setStoreId(teamIdStoreResId)
        activityViewModel.setStoreName(storeName)
        Log.d(TAG, "storeName: $storeName")
        val restaurantData = RestaurantData(storeName, teamIdStoreResId)
        restaurantDetailsViewModel.setRestaurantData(restaurantData)
        mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.RESTAURANT_DETAILS_FRAGMENT)
    }

    override fun onManageClick(teamUserRes: TeamUserRes) {
        showAuthoritySettingDialog(teamUserRes)
    }

    override fun onResignClick(teamUserRes: TeamUserRes) {
        showGroupResignDialog(teamUserRes)
    }

    /**
     * QR 코드를 생성하여 다이얼로그로 표시하는 함수
     *
     * @param context 다이얼로그를 표시할 컨텍스트 (Fragment 내에서는 requireContext()를 사용)
     * @param url QR 코드에 인코딩할 URL (기본값: "https://www.naver.com")
     */
    fun showQRDialog(url: String = "https://www.naver.com") {
        val context = this@GroupDetailsFragment.requireContext()

        // 타이머 생성
        val timer = Timer()

        // 다이얼로그 레이아웃 인플레이트
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_qr, null)

        // QR 코드 생성 및 이미지뷰에 적용
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(url, BarcodeFormat.QR_CODE, 400, 400)
            val imageViewQrCode = dialogView.findViewById<ImageView>(R.id.imageViewQrCode)
            imageViewQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("QRDialog", "QR 코드 생성 실패", e)
        }

        // AlertDialog 생성
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)  // 뒤로 가기 버튼 허용
            .create()


        // 다이얼로그 닫힐 때 타이머 취소
        dialog.setOnDismissListener {
            timer.cancel()
            initialView()
        }

        // 다이얼로그 표시
        dialog.show()

        // 60초 카운트다운 타이머 시작
        var seconds = 60
        val qrTimer = dialogView.findViewById<TextView>(R.id.qr_timer)
        qrTimer?.text = "남은 시간: 60초"

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                seconds--
                // UI 업데이트는 메인 스레드에서 수행
                this@GroupDetailsFragment.requireActivity().runOnUiThread {
                    qrTimer?.text = "남은 시간: ${seconds}초"
                }
                if (seconds <= 0) {
                    // 시간이 다 되었으면 다이얼로그를 닫고 타이머 취소
                    this@GroupDetailsFragment.requireActivity().runOnUiThread {
                        if (dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }
                    timer.cancel()
                    initialView()
                }
            }
        }, 1000, 1000)
    }

    //다이얼로그 보여주는 부분
    private fun showInviteCodeInputDialog() {
        val binding = DialogInviteCodeBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        Log.d(TAG,"초대코드"+inviteCode)
        binding.etInviteCode.text = inviteCode
        binding.inviteCodeConfirmBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showGroupExitDialog() {
        val binding = DialogGroupExitBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        binding.groupExitConfirmBtn.setOnClickListener {
            val tr = TeamIdReq(teamId = activityViewModel.teamId.value!!.toInt())
            exitTeam(tr)
            mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.MYPAGE_FRAGMENT)
            dialog.dismiss()
        }

        binding.groupExitCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    private fun showMoneyChangeDialog() {
        val binding = DialogMoneyChangeBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        binding.btnRegister.setOnClickListener {
            val moneyInput = binding.etCodeInput.text.toString()
            val moneyValue = if (moneyInput.isEmpty()) {
                10000
            } else {
                moneyInput.toInt()
            }
            val moneychange = MoneyChangeReq(moneyValue,activityViewModel.teamId.value!!.toInt())
            moneyChange(moneychange)
            viewModel.setMoneyValue(moneychange.dailyPriceLimit)
            dialog.dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showGroupResignDialog(ban: TeamUserRes) {
        val binding = DialogGroupResignBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        binding.groupResignConfirmBtn.setOnClickListener {
            val banUser = BanUserReq(ban.email,ban.teamId)
            viewModel.TeamResign(SharedPreferencesUtil.getAccessToken()!!,banUser)
            dialog.dismiss()
        }

        binding.groupResignCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAuthoritySettingDialog(privilege : TeamUserRes) {
        val binding = DialogAuthoritySettingBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
        binding.autoritySettingConfirmBtn.setOnClickListener {
            val pr = PrivilegeUserReq(privilege.email,true,privilege.teamId)
            privilegeUser(pr)
            showToast(privilege.nickname+"님에게 권한을 부여하였습니다.")
            dialog.dismiss()
        }

        binding.autoritySettingCancelBtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    fun moneyChange(moneychange: MoneyChangeReq){
        lifecycleScope.launch {
            runCatching {
                RetrofitUtil.teamService.moneyChange(SharedPreferencesUtil.getAccessToken()!!,moneychange)
            }.onSuccess {

            }.onFailure {

            }
        }
    }

    fun privilegeUser(pr:PrivilegeUserReq){
        lifecycleScope.launch {
            runCatching {
              RetrofitUtil.teamService.privilegeUser(SharedPreferencesUtil.getAccessToken()!!,pr)
            }.onSuccess {

            }.onFailure {

            }
        }
    }

    fun exitTeam(tr: TeamIdReq){
        lifecycleScope.launch {
            runCatching {
                RetrofitUtil.teamService.exitTeam(SharedPreferencesUtil.getAccessToken()!!,tr)
            }.onSuccess {

            }.onFailure {

            }
        }
    }
}

