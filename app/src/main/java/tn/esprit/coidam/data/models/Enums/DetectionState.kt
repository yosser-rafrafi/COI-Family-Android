package tn.esprit.coidam.data.models.Enums

/**
 * États de la machine de détection AutoBlind
 */
enum class DetectionState {
    IDLE,                    // Aucun visage détecté
    FACE_DETECTED,          // Visage détecté, en attente de stabilisation
    FACE_STABLE,            // Visage stable ≥ 1.5s, annonce "Visage détecté"
    RECOGNIZING,            // Reconnaissance en cours (silencieux)
    PERSON_IDENTIFIED       // Personne identifiée, annonce faite, en cooldown
}
