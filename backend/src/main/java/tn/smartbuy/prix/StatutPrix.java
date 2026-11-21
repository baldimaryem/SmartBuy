package tn.smartbuy.prix;

/** Un prix proposé par un utilisateur reste EN_ATTENTE jusqu'à la décision d'un administrateur (PRX-08). */
public enum StatutPrix {
    VALIDE,
    EN_ATTENTE,
    REJETE
}
