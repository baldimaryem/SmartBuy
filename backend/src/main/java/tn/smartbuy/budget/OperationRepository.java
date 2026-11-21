package tn.smartbuy.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    List<Operation> findByUtilisateurIdOrderByDateOperationDescIdDesc(Long utilisateurId);

    /** Opérations d'une période (ex. un mois : du 1er au dernier jour), les plus récentes d'abord. */
    List<Operation> findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
            Long utilisateurId, LocalDate debut, LocalDate fin);

    /** Opérations récurrentes de tous les utilisateurs sur une période (BUD-07). */
    List<Operation> findByRecurrenteAndDateOperationBetween(boolean recurrente, LocalDate debut, LocalDate fin);

    boolean existsByUtilisateurIdAndCategorieIdAndMontantAndDateOperation(
            Long utilisateurId, Long categorieId, BigDecimal montant, LocalDate dateOperation);

    boolean existsByCategorieId(Long categorieId);

    /** Total des opérations d'un type sur une période (SUM calculé par Oracle). */
    @Query("""
            select coalesce(sum(o.montant), 0) from Operation o
            where o.utilisateurId = :utilisateurId and o.type = :type
              and o.dateOperation between :debut and :fin
            """)
    BigDecimal total(@Param("utilisateurId") Long utilisateurId, @Param("type") TypeOperation type,
                     @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    /** Total des dépenses d'une catégorie sur une période. */
    @Query("""
            select coalesce(sum(o.montant), 0) from Operation o
            where o.utilisateurId = :utilisateurId and o.type = tn.smartbuy.budget.TypeOperation.DEPENSE
              and o.categorie.id = :categorieId
              and o.dateOperation between :debut and :fin
            """)
    BigDecimal totalDepensesCategorie(@Param("utilisateurId") Long utilisateurId,
                                      @Param("categorieId") Long categorieId,
                                      @Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
