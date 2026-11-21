package tn.smartbuy.prix;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.smartbuy.commun.Montants;

import java.time.LocalDate;

/** Objets JSON du module « comparaison des prix » (noms de champs = contrat d'API). */
public final class PrixDtos {

    private PrixDtos() {
    }

    // ---------- Magasins ----------

    public record MagasinRequete(
            @NotBlank(message = "Le nom du magasin est obligatoire") @Size(max = 100) String nom,
            @NotBlank(message = "L'enseigne est obligatoire") @Size(max = 60) String enseigne,
            @Size(max = 200) String adresse,
            @Size(max = 60) String ville) {
    }

    public record MagasinDto(Long id, String nom, String enseigne, String adresse, String ville,
                             boolean enLigne, String siteWeb) {

        public MagasinDto(Long id, String nom, String enseigne, String adresse, String ville) {
            this(id, nom, enseigne, adresse, ville, false, null);
        }

        public static MagasinDto depuis(Magasin m) {
            return new MagasinDto(m.getId(), m.getNom(), m.getEnseigne(), m.getAdresse(), m.getVille(),
                    m.isEnLigne(), m.getSiteWeb());
        }
    }

    // ---------- Catégories de produits ----------

    public record CategorieProduitRequete(
            @NotBlank(message = "Le nom de la catégorie est obligatoire") @Size(max = 60) String nom,
            Univers univers) {
    }

    public record CategorieProduitDto(Long id, String nom, Univers univers) {

        public static CategorieProduitDto depuis(CategorieProduit c) {
            return new CategorieProduitDto(c.getId(), c.getNom(), c.getUnivers());
        }
    }

    // ---------- Produits ----------

    public record ProduitRequete(
            @NotBlank(message = "Le nom du produit est obligatoire") @Size(max = 150) String nom,
            @Size(max = 60) String marque,
            @Size(max = 20, message = "Code-barres trop long") String codeBarres,
            @NotBlank(message = "L'unité est obligatoire (kg, L, pièce)") @Size(max = 10) String unite,
            @NotNull(message = "La catégorie du produit est obligatoire") Long categorieProduitId,
            @Size(max = 500) String imageUrl) {
    }

    /** Objet « Produit » du contrat : meilleurPrix / meilleurMagasinNom à null si aucun prix validé. */
    public record ProduitDto(Long id, String nom, String marque, String codeBarres, String unite,
                             Long categorieProduitId, String categorieProduitNom, String imageUrl,
                             Double meilleurPrix, String meilleurMagasinNom,
                             Univers univers, SourceProduit source, String urlSource) {

        public static ProduitDto depuis(Produit p, Prix meilleur) {
            return new ProduitDto(p.getId(), p.getNom(), p.getMarque(), p.getCodeBarres(), p.getUnite(),
                    p.getCategorieProduit().getId(), p.getCategorieProduit().getNom(), p.getImageUrl(),
                    meilleur == null ? null : Montants.versDouble(meilleur.getMontant()),
                    meilleur == null ? null : meilleur.getMagasin().getNom(),
                    p.getUnivers(), p.getSource(), p.getUrlSource());
        }
    }

    /** Objet « PrixMagasin » : dernier prix VALIDE d'un produit dans un magasin (PRX-04). */
    public record PrixMagasinDto(Long prixId, Long magasinId, String magasinNom, String enseigne,
                                 Double montant, LocalDate dateReleve, boolean meilleur, SourcePrix source,
                                 boolean enLigne) {
    }

    /** Un point de l'historique des prix (PRX-05). */
    public record HistoriquePrixDto(String magasinNom, Double montant, LocalDate dateReleve) {
    }

    /** Réponse de GET /produits/code-barres/{code}. */
    public record ResultatCodeBarresDto(ProduitDto produit, String origine, String message) {
    }

    /** Produit ajouté par un utilisateur qui ne le trouvait pas (code-barres inconnu). */
    public record ContributionProduitRequete(
            @NotBlank(message = "Le nom du produit est obligatoire") @Size(max = 150) String nom,
            @Size(max = 60) String marque,
            @Size(max = 20, message = "Code-barres trop long") String codeBarres,
            @NotBlank(message = "L'unité est obligatoire (kg, L, pièce)") @Size(max = 10) String unite,
            @NotNull(message = "Le rayon est obligatoire") Long categorieProduitId) {
    }

    // ---------- Prix (propositions / administration) ----------

    public record PrixRequete(
            @NotNull(message = "Le produit est obligatoire") Long produitId,
            @NotNull(message = "Le magasin est obligatoire") Long magasinId,
            @NotNull(message = "Le montant est obligatoire") @Positive(message = "Le montant doit être positif") Double montant,
            LocalDate dateReleve) {
    }

    /** Objet « Prix » du contrat. */
    public record PrixDto(Long id, Long produitId, String produitNom, Long magasinId, String magasinNom,
                          Double montant, LocalDate dateReleve, String source, String statut, String proposePar) {

        public static PrixDto depuis(Prix p) {
            return new PrixDto(p.getId(), p.getProduit().getId(), p.getProduit().getNom(),
                    p.getMagasin().getId(), p.getMagasin().getNom(), Montants.versDouble(p.getMontant()),
                    p.getDateReleve(), p.getSource().name(), p.getStatut().name(),
                    p.getProposePar() == null ? null : p.getProposePar().getNom());
        }
    }
}
