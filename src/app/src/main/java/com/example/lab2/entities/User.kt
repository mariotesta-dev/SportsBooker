package com.example.lab2.entities

import com.example.lab2.utils.DateAsLongSerializer
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Date

@Serializable
data class User(
    @Exclude
    val userId: String,
    val full_name: String,
    val nickname: String,
    val address: String,
    val description: String,
    val email: String,
    var image: String,
    @Serializable(with = DateAsLongSerializer::class)
    var birthday: LocalDate,
    var badges: Map<BadgeType, Int>,
    var interests: MutableList<Sport>,
    var score: MutableMap<String, Long>? = null
) {

    fun getAge(): Int {
        val today = LocalDate.now()
        val period = Period.between(birthday, today)
        return period.years
    }

    fun toJson(): String =
        Json.encodeToString(this)

    companion object {

        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(jsonString: String): User =
            json.decodeFromString<User>(jsonString)


        fun fromFirebase(data: DocumentSnapshot): User {

            val skills = data.get("skills") as Map<String, Int>
            val mappedBadges: Map<BadgeType, Int> = skills.mapKeys {
                when (it.key.lowercase()) {
                    "speed" -> BadgeType.SPEED
                    "precision" -> BadgeType.PRECISION
                    "team work" -> BadgeType.TEAM_WORK
                    "strategy" -> BadgeType.STRATEGY
                    "endurance" -> BadgeType.ENDURANCE
                    else -> BadgeType.SPEED// TODO can be mapped to an UNKNOWN instead
                }
            }

            val interests = data.get("interests") as List<String>
            val mappedInterests: MutableList<Sport> = interests.map {
                when (it.lowercase()) {
                    "football" -> Sport.FOOTBALL
                    "padel" -> Sport.PADEL
                    "tennis" -> Sport.TENNIS
                    "basketball" -> Sport.BASKETBALL
                    "baseball" -> Sport.BASEBALL
                    "golf" -> Sport.GOLF
                    else -> Sport.FOOTBALL// TODO can be mapped to an UNKNOWN instead
                }
            }.toMutableList()

            return User(
                userId = data.id,
                full_name = data.getString("fullName")!!,
                nickname = data.getString("username")!!,
                address = data.getString("location")!!,
                description = data.getString("description")!!,
                email = data.getString("email")!!,
                birthday = data.getTimestamp("dateOfBirth")?.toDate()?.toInstant()
                    ?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now(),
                badges = mappedBadges,
                interests = mappedInterests,
                image = data.getString("image")!!,
                score = data.get("score") as MutableMap<String, Long>?
            )
        }

        fun toFirebase(user: User): Map<String, Any> {

            val mappedBirthday: Date =
                Date.from(user.birthday.atStartOfDay(ZoneId.systemDefault()).toInstant())

            val mappedBadges: Map<String, Int> = user.badges.mapKeys {
                when (it.key) {
                    BadgeType.SPEED -> "Speed"
                    BadgeType.PRECISION -> "Precision"
                    BadgeType.TEAM_WORK -> "Team Work"
                    BadgeType.STRATEGY -> "Strategy"
                    BadgeType.ENDURANCE -> "Endurance"
                    else -> "Unknown"
                }
            }

            val mappedInterests: List<String> = user.interests.map {
                when (it) {
                    Sport.FOOTBALL -> "Football"
                    Sport.PADEL -> "Padel"
                    Sport.TENNIS -> "Tennis"
                    Sport.BASKETBALL -> "Basketball"
                    Sport.BASEBALL -> "Baseball"
                    Sport.GOLF -> "Golf"
                    else -> "Unknown"
                }
            }

            val map: MutableMap<String, Any> = mutableMapOf()

            map["fullName"] = user.full_name
            map["username"] = user.nickname
            map["location"] = user.address
            map["description"] = user.description
            map["email"] = user.email
            map["dateOfBirth"] = Timestamp(mappedBirthday)
            map["skills"] = mappedBadges
            map["interests"] = mappedInterests
            map["image"] = user.image
            map["score"] = user.score ?: mutableMapOf<String, Long>()

            return map
        }

    }
}
