package com.example.lab2.view_models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.lab2.entities.Equipment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EquipmentsVM @Inject constructor(

) : ViewModel() {


    private var personalPrice = MutableLiveData<Double>()
    fun getPersonalPrice(): LiveData<Double> {
        return personalPrice
    }

    fun setPersonalPrice(value: Double) {
        personalPrice.value = value
    }

    companion object {
        private val SPORT_EQUIPMENT_MAP = mapOf(
            "Tennis" to listOf(
                Equipment("Racket", 2.0),
                Equipment("Tennis balls", 1.5)
            ),
            "Football" to listOf(
                Equipment("Football ball", 5.0),
                Equipment("Shin guards", 3.5),
                Equipment("Cleats", 2.5)
            ),
            "Golf" to listOf(
                Equipment("Golf clubs", 5.5),
                Equipment("Golf balls", 1.5),
                Equipment("Golf cart", 30.0)
            ),
            "Baseball" to listOf(
                Equipment("Baseball bat", 10.0),
                Equipment("Baseball gloves", 2.0),
                Equipment("Baseballs", 2.0)
            ),
            "Basketball" to listOf(
                Equipment("Basketball shoes", 5.0),
                Equipment("Basketball jersey", 5.0),
                Equipment("Basketballs", 1.0)
            ),
            "Padel" to listOf(
                Equipment("Padel racket", 5.0),
                Equipment("Padel balls", 1.0)
            )
        )
    }

    fun getListEquipments(sport: String): List<Equipment> {
        return SPORT_EQUIPMENT_MAP[sport] ?: emptyList()
    }


    private var equipments = MutableLiveData<MutableList<Equipment>>()
    fun getEquipments(): LiveData<MutableList<Equipment>> {
        return equipments
    }

    fun setEquipments(equipments: MutableList<Equipment>) {
        this.equipments.value = equipments
    }

    fun addEquipment(e: Equipment) {
        equipments.value!!.add(e)
    }

    fun removeEquipment(e: Equipment) {
        equipments.value!!.remove(e)
    }
}