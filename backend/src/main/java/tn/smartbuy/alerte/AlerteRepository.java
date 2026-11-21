package tn.smartbuy.alerte;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    List<Alerte> findByUtilisateurIdOrderByDateAlerteDescIdDesc(Long utilisateurId);

    long countByUtilisateurIdAndLue(Long utilisateurId, boolean lue);

    boolean existsByBudgetIdAndType(Long budgetId, TypeAlerte type);
}
