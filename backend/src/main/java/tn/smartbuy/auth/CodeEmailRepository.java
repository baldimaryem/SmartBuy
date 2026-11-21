package tn.smartbuy.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodeEmailRepository extends JpaRepository<CodeEmail, Long> {

    /** Le dernier code demandé pour ce but et pas encore utilisé. */
    Optional<CodeEmail> findFirstByUtilisateurIdAndButAndUtiliseFalseOrderByIdDesc(Long utilisateurId, CodeEmail.But but);

    List<CodeEmail> findByUtilisateurIdAndButAndUtiliseFalse(Long utilisateurId, CodeEmail.But but);
}
