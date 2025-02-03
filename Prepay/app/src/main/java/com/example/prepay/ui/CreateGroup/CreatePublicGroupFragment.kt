package com.example.prepay.ui.CreateGroup

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import com.example.prepay.BaseFragment
import com.example.prepay.CommonUtils
import com.example.prepay.R
import com.example.prepay.data.model.dto.PublicPrivateTeam
import com.example.prepay.databinding.FragmentCreatePublicGroupBinding
import com.example.prepay.ui.MainActivity

private const val TAG = "CreatePublicGroupFragme"
class CreatePublicGroupFragment: BaseFragment<FragmentCreatePublicGroupBinding>(
    FragmentCreatePublicGroupBinding::bind,
    R.layout.fragment_create_public_group
){
    private lateinit var mainActivity: MainActivity
    private var isCheckingRepeatUse = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = context as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEvent()
    }

    fun initEvent(){
        binding.publicCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.privateCheckbox.isChecked = false
                mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.CREATE_PUBLIC_GROUP_FRAGMENT)
            }
        }
        binding.privateCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.publicCheckbox.isChecked = false
                mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.CREATE_PRIVATE_GROUP_FRAGMENT)
            }
        }
        binding.possible.setOnCheckedChangeListener{ _, isChecked ->
            if (isCheckingRepeatUse) return@setOnCheckedChangeListener
            this.isCheckingRepeatUse = true
            if (isChecked) {
                binding.impossible.isChecked = false
            }
            isCheckingRepeatUse = false
        }

        binding.impossible.setOnCheckedChangeListener { _, isChecked ->
            if (isCheckingRepeatUse) return@setOnCheckedChangeListener
            isCheckingRepeatUse = true
            if(isChecked) {
                binding.possible.isChecked = false
            }
            isCheckingRepeatUse = false
        }

        binding.test.setOnClickListener {
            mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.BOOTPAY_FRAGMENT)
        }

        binding.searchRestaurant.isSubmitButtonEnabled = true
        binding.searchRestaurant.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d(TAG, "onQueryTextSubmit: $query")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })

        binding.registerBtn.setOnClickListener {
            var repeat_use_num = 0
            val public_team = if (binding.publicCheckbox.isChecked) 0 else 1
            val team_name = binding.groupNameText.getText().toString()
            val image_url = binding.imageBtn.urls
            val search_restaurant = binding.searchRestaurant.toString()
            val daily_price_limit = binding.limitSettingText.getText().toString()
            val repeat_use_possible = binding.possible.isChecked
            val repeat_use_impossible = binding.impossible.isChecked
            if (repeat_use_possible) {
                repeat_use_num = 0
            }
            if (repeat_use_impossible) {
                repeat_use_num = 1
            }

            val context_text = binding.textInputText.getText().toString()

            // POST로 넘기기
            Log.d(TAG, "public_team: $public_team")
            Log.d(TAG, "team_name: $team_name")
            Log.d(TAG, "search_restaurant: $search_restaurant")
            Log.d(TAG, "daily_price_limit: $daily_price_limit")
            Log.d(TAG, "repeat_use_possible: $repeat_use_possible")
            Log.d(TAG, "repeat_use_impossible: $repeat_use_impossible")
            Log.d(TAG, "repeat_use_num: $repeat_use_num")
            Log.d(TAG, "image_url: $image_url")
            Log.d(TAG, "context_text: $context_text")

            PublicPrivateTeam(public_team ,team_name, daily_price_limit.toInt(), repeat_use_num, image_url.toString(), context_text)
            mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.MYPAGE_FRAGMENT)
        }
        binding.cancelBtn.setOnClickListener {
            mainActivity.changeFragmentMain(CommonUtils.MainFragmentName.MYPAGE_FRAGMENT)
        }
        //내비게이션 바 없어지게 mainActivity에 함수가 있습니다.
        mainActivity.hideBottomNav(true)
    }
}