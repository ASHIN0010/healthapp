package com.example.healthapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TranslationManager {
    private val _currentLanguage = MutableStateFlow("pa") // Default Punjabi (Nabha adoption)
    val currentLanguage: StateFlow<String> = _currentLanguage

    val supportedLanguages = listOf(
        Language("pa", "ਪੰਜਾਬੀ", "🇮🇳"),
        Language("hi", "हिंदी", "🇮🇳"),
        Language("en", "English", "🇺🇸"),
        Language("kn", "Kannada", "⚓"),
        Language("ta", "Tamil", "🕉️"),
        Language("ml", "Malayalam", "🌴")
    )

    data class Language(val code: String, val name: String, val flag: String)

    fun setLanguage(code: String) {
        if (supportedLanguages.any { it.code == code }) {
            _currentLanguage.value = code
        }
    }

    fun getString(key: String): String {
        return translations[_currentLanguage.value]?.get(key) ?: translations["en"]?.get(key) ?: key
    }

    private val translations = mapOf(
        "en" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "Hospital Dashboard",
            "patient_dashboard" to "Patient Dashboard",
            "find_doctor" to "Find Doctor",
            "symptom_checker" to "Symptom Checker",
            "pharmacy" to "Pharmacy",
            "book" to "Book",
            "available" to "Available",
            "unavailable" to "Unavailable",
            "emergency_control" to "Emergency Control",
            "upcoming_appointments" to "Upcoming OPD Appointments",
            "doctors_management" to "Doctors & OPD Management",
            "select_language" to "Select Language",
            "logout" to "Logout"
        ),
        "pa" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "ਹਸਪਤਾਲ ਡੈਸ਼ਬੋਰਡ",
            "patient_dashboard" to "ਮਰੀਜ਼ ਡੈਸ਼ਬੋਰਡ",
            "find_doctor" to "ਡਾਕਟਰ ਲੱਭੋ",
            "symptom_checker" to "ਲੱਛਣ ਜਾਂਚਕਰਤਾ",
            "pharmacy" to "ਫਾਰਮੇਸੀ",
            "book" to "ਪੱਕਾ ਕਰੋ", // Booking/Confirm
            "available" to "ਉਪਲਬਧ",
            "unavailable" to "ਉਪਲਬਧ ਨਹੀਂ",
            "emergency_control" to "ਐਮਰਜੈਂਸੀ ਕੰਟਰੋਲ",
            "upcoming_appointments" to "ਆਉਣ ਵਾਲੀਆਂ ਮੁਲਾਕਾਤਾਂ",
            "doctors_management" to "ਡਾਕਟਰ ਪ੍ਰਬੰਧਨ",
            "select_language" to "ਭਾਸ਼ਾ ਚੁਣੋ",
            "logout" to "ਬਾਹਰ ਜਾਓ"
        ),
        "hi" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "अस्पताल डैशबोर्ड",
            "patient_dashboard" to "रोगी डैशबोर्ड",
            "find_doctor" to "डॉक्टर खोजें",
            "symptom_checker" to "लक्षण जांचकर्ता",
            "pharmacy" to "फार्मेसी",
            "book" to "बुक करें",
            "available" to "उपलब्ध",
            "unavailable" to "अनुपलब्ध",
            "emergency_control" to "आपातकालीन नियंत्रण",
            "upcoming_appointments" to "आगामी ओपीडी नियुक्तियां",
            "doctors_management" to "डॉक्टर और ओपीडी प्रबंधन",
            "select_language" to "भाषा चुनें",
            "logout" to "लॉग आउट"
        ),
        "kn" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "ಆಸ್ಪತ್ರೆ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
            "patient_dashboard" to "ರೋಗಿಯ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್",
            "find_doctor" to "ವೈದ್ಯರನ್ನು ಹುಡುಕಿ",
            "symptom_checker" to "ರೋಗಲಕ್ಷಣ ಪರೀಕ್ಷಕ",
            "pharmacy" to "ಔಷಧಾಲಯ",
            "book" to "ಬುಕ್ ಮಾಡಿ",
            "available" to "ಲಭ್ಯವಿದೆ",
            "unavailable" to "ಲಭ್ಯವಿಲ್ಲ",
            "emergency_control" to "ತುರ್ತು ನಿಯಂತ್ರಣ",
            "upcoming_appointments" to "ಮುಂಬರುವ ಒಪಿಡಿ ನೇಮಕಾತಿಗಳು",
            "doctors_management" to "ವೈದ್ಯರು ಮತ್ತು ಒಪಿಡಿ ನಿರ್ವಹಣೆ",
            "select_language" to "ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ",
            "logout" to "ಲಾಗ್ ಔಟ್"
        ),
        "ta" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "மருத்துவமனை டாஷ்போர்டு",
            "patient_dashboard" to "நோயாளி டாஷ்போர்டு",
            "find_doctor" to "மருத்துவரைத் தேடுங்கள்",
            "symptom_checker" to "அறிகுறி சரிபார்ப்பு",
            "pharmacy" to "மருந்தகம்",
            "book" to "முன்பதிவு",
            "available" to "கிடைக்கிறது",
            "unavailable" to "கிடைக்கவில்லை",
            "emergency_control" to "அவசர கட்டுப்பாடு",
            "upcoming_appointments" to "வரவிருக்கும் OPD சந்திப்புகள்",
            "doctors_management" to "மருத்துவர்கள் & OPD மேலாண்மை",
            "select_language" to "மொழியைத் தேர்ந்தெடுக்கவும்",
            "logout" to "வெளியேறு"
        ),
        "ml" to mapOf(
            "app_name" to "HealNabha",
            "hospital_dashboard" to "ആശുപത്രി ഡാഷ്ബോർഡ്",
            "patient_dashboard" to "രോഗി ഡാഷ്ബോർഡ്",
            "find_doctor" to "ഡോക്ടറെ കണ്ടെത്തുക",
            "symptom_checker" to "രോഗലക്ഷണ പരിശോധന",
            "pharmacy" to "ഫാർമസി",
            "book" to "ബുക്ക് ചെയ്യുക",
            "available" to "ലഭ്യമാണ്",
            "unavailable" to "ലഭ്യമല്ല",
            "emergency_control" to "അടിയന്തര നിയന്ത്രണം",
            "upcoming_appointments" to "വരാനിരിക്കുന്ന OPD കൂടിക്കാഴ്ചകൾ",
            "doctors_management" to "ഡോക്ടർമാർ & OPD മാനേജ്‌മെന്റ്",
            "select_language" to "ഭാഷ തിരഞ്ഞെടുക്കുക",
            "logout" to "ലോഗ് ഔട്ട്"
        )
    )
}
