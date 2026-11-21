package tn.smartbuy.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.smartbuy.commun.Montants;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Objets JSON du module « Smart Budget » (noms de champs = contrat d'API). */
public final class BudgetDtos {

    private BudgetDtos() {
    }

    // ---------- Catégories ----------

    public record CategorieRequete(
            @NotBlank(message = "Le nom de la catégorie est obligatoire") @Size(max = 60) String nom,
            @NotNull(message = "Le type est obligatoire (DEPENSE ou REVENU)") TypeOperation type,
            @Size(max = 50) String icone) {
    }

    public record CategorieDto(Long id, String nom, TypeOperation type, String icone, boolean personnalisee) {

        public static CategorieDto depuis(Categorie c) {
            return new CategorieDto(c.getId(), c.getNom(), c.getType(), c.getIcone(), c.estPersonnalisee());
        }
    }

    // ---------- Opérations ----------

    public record OperationRequete(
            @NotNull(message = "Le type est obligatoire (DEPENSE ou REVENU)") TypeOperation type,
            @NotNull(message = "Le montant est obligatoire") @Positive(message = "Le montant doit être positif") Double montant,
            @NotNull(message = "La date est obligatoire (AAAA-MM-JJ)") LocalDate dateOperation,
            @Size(max = 255, message = "La note est trop longue") String note,
            @NotNull(message = "La catégorie est obligatoire") Long categorieId,
            Long magasinId,
            Boolean recurrente) {
    }

    public record OperationDto(Long id, TypeOperation type, Double montant, LocalDate dateOperation, String note,
                               Long categorieId, String categorieNom, Long magasinId, String magasinNom,
                               boolean recurrente, Double economie) {

        public static OperationDto depuis(Operation o) {
            return new OperationDto(o.getId(), o.getType(), Montants.versDouble(o.getMontant()), o.getDateOperation(),
                    o.getNote(), o.getCategorie().getId(), o.getCategorie().getNom(),
                    o.getMagasin() == null ? null : o.getMagasin().getId(),
                    o.getMagasin() == null ? null : o.getMagasin().getNom(),
                    o.isRecurrente(), Montants.versDouble(Montants.nonNul(o.getEconomie())));
        }
    }

    // ---------- Budgets ----------

    public record BudgetRequete(
            @NotNull(message = "Le mois est obligatoire")
            @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "Mois invalide : utilisez le format AAAA-MM")
            String mois,
            Long categorieId,
            @NotNull(message = "Le plafond est obligatoire") @Positive(message = "Le plafond doit être positif")
            Double montantPlafond) {
    }

    /** Objet « Budget » : categorieId null = budget global. */
    public record BudgetDto(Long id, String mois, Long categorieId, String categorieNom, Double montantPlafond,
                            Double depense, Double reste, Double pourcentage) {

        public static BudgetDto depuis(Budget b, BigDecimal depense) {
            BigDecimal plafond = b.getMontantPlafond();
            return new BudgetDto(b.getId(), b.getMois().toString(),
                    b.estGlobal() ? null : b.getCategorie().getId(),
                    b.estGlobal() ? null : b.getCategorie().getNom(),
                    Montants.versDouble(plafond), Montants.versDouble(depense),
                    Montants.versDouble(plafond.subtract(depense)),
                    Montants.pourcentage(depense, plafond));
        }
    }

    // ---------- Tableau de bord ----------

    public record PrevisionDto(Double depenseEstimeeFinMois, boolean depassementProbable) {
    }

    public record RepartitionDto(Long categorieId, String categorieNom, Double montant) {
    }

    public record EvolutionDto(String mois, Double depenses, Double revenus) {
    }

    public record DashboardDto(String mois, Double totalRevenus, Double totalDepenses, Double solde,
                               Double budgetGlobal, Double resteADepenser, PrevisionDto prevision,
                               List<RepartitionDto> repartition, List<EvolutionDto> evolution,
                               Double economiesRealisees, long alertesNonLues) {
    }
}
