package tn.smartbuy.liste;

import tn.smartbuy.prix.Univers;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.smartbuy.liste.ComparateurPrix.ResultatMagasin;

import java.time.LocalDate;
import java.util.List;

/** Objets JSON des listes de courses (noms de champs = contrat d'API). */
public final class ListeDtos {

    private ListeDtos() {
    }

    public record ListeRequete(
            @NotBlank(message = "Le nom de la liste est obligatoire") @Size(max = 100) String nom,
            Univers univers) {
    }

    public record LigneRequete(
            @NotNull(message = "Le produit est obligatoire") Long produitId,
            @NotNull(message = "La quantité est obligatoire") @Positive(message = "La quantité doit être positive")
            @Max(value = 999, message = "Quantité trop grande") Integer quantite) {
    }

    public record QuantiteRequete(
            @NotNull(message = "La quantité est obligatoire") @Positive(message = "La quantité doit être positive")
            @Max(value = 999, message = "Quantité trop grande") Integer quantite) {
    }

    public record AchatRequete(@NotNull(message = "Le magasin est obligatoire") Long magasinId) {
    }

    /** Liste « résumée » (GET /listes). */
    public record ListeResumeDto(Long id, String nom, LocalDate dateCreation, StatutListe statut, int nbArticles,
                                 Univers univers) {

        public static ListeResumeDto depuis(ListeCourses l) {
            return new ListeResumeDto(l.getId(), l.getNom(), l.getDateCreation(), l.getStatut(), l.getLignes().size(),
                    l.getUnivers());
        }
    }

    public record LigneDto(Long id, Long produitId, String produitNom, String unite, int quantite) {

        public static LigneDto depuis(LigneListe l) {
            return new LigneDto(l.getId(), l.getProduit().getId(), l.getProduit().getNom(), l.getProduit().getUnite(),
                    l.getQuantite());
        }
    }

    /** Liste « détaillée » (GET /listes/{id}). */
    public record ListeDetailDto(Long id, String nom, StatutListe statut, LocalDate dateCreation, List<LigneDto> lignes,
                                 Univers univers) {

        public static ListeDetailDto depuis(ListeCourses l) {
            return new ListeDetailDto(l.getId(), l.getNom(), l.getStatut(), l.getDateCreation(),
                    l.getLignes().stream().map(LigneDto::depuis).toList(), l.getUnivers());
        }
    }

    /** Réponse de GET /listes/{id}/comparaison. */
    public record ComparaisonDto(Long listeId, List<ResultatMagasin> magasins, Long magasinRecommandeId,
                                 Double economie, Double budgetRestant, boolean depasseBudget) {
    }
}
