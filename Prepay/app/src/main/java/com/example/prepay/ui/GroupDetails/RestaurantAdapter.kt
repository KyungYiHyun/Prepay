package com.example.prepay.ui.GroupDetails

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.prepay.data.model.dto.Restaurant
import com.example.prepay.databinding.ItemRestaurantBinding

class RestaurantAdapter(private val restaurantList: List<Restaurant>,private val listener: OnRestaurantClickListener) :
    RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    class RestaurantViewHolder(private val binding: ItemRestaurantBinding,private val listener: OnRestaurantClickListener) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(restaurant: Restaurant) {
            binding.restaurantName.text = restaurant.name
            binding.restaurantPrepayMoney.text = restaurant.prepayMoney.toString()+"원"

            binding.restaurantDetailBtn.setOnClickListener {
                listener.onRestaurantClick(restaurant)
            }
        }
    }

    // 뷰홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val binding = ItemRestaurantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RestaurantViewHolder(binding,listener)
    }

    // view에 바인딩
    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val currentRestaurant = restaurantList[position]
        holder.bind(currentRestaurant)
    }

    // 갯수 반환
    override fun getItemCount(): Int {
        return restaurantList.size
    }

    interface OnRestaurantClickListener {
        fun onRestaurantClick(restaurant: Restaurant)
    }
}