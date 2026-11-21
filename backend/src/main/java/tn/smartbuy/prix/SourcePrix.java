package tn.smartbuy.prix;

/** Qui a saisi le prix : un administrateur (ADM-04) ou un utilisateur (PRX-07). */
public enum SourcePrix {
    ADMIN,
    UTILISATEUR,
    /** relevé automatiquement sur le site d'une enseigne (web scraping / API publique) */
    COLLECTE
}
