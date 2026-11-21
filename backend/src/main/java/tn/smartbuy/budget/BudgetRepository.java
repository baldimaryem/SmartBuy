package tn.smartbuy.budget;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUtilisateurIdAndMois(Long utilisateurId, YearMonth mois);

    /** Budget global du mois (categorie_id IS NULL). */
    Optional<Budget> findByUtilisateurIdAndMoisAndCategorieIsNull(Long utilisateurId, YearMonth mois);

    Optional<Budget> findByUtilisateurIdAndMoisAndCategorieId(Long utilisateurId, YearMonth mois, Long categorieId);
}
