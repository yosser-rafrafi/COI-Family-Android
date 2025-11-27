package tn.esprit.coidam.data.models.Enums

import com.google.gson.annotations.SerializedName

enum class CallType {
    @SerializedName("audio") AUDIO,
    @SerializedName("video") VIDEO;

    fun displayName(): String = when (this) {
        AUDIO -> "Audio"
        VIDEO -> "Vidéo"
    }
}