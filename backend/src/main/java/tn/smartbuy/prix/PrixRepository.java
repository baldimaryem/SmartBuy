package tn.smartbuy.prix;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrixRepository extends JpaRepository<Prix, Long> {

    /** Tous les relevés d'un produit avec un statut donné, du plus ancien au plus récent. */
    List<Prix> findByProduitIdAndStatutOrderByDateReleveAscIdAsc(Long produitId, StatutPrix statut);

    /** Relevés de plusieurs produits d'un coup (évite une requête par produit). */
    List<Prix> findByProduitIdInAndStatut(Collection<Long> produitIds, StatutPrix statut);

    List<Prix> findByStatut(StatutPrix statut);

    /** Prix collecté aujourd'hui pour ce produit dans ce magasin (pour le mettre à jour au lieu de le dupliquer). */
    java.util.Optional<Prix> findFirstByProduitIdAndMagasinIdAndSourceAndDateReleve(
            Long produitId, Long magasinId, SourcePrix source, java.time.LocalDate dateReleve);

    List<Prix> findByStatutOrderByDateReleveAscIdAsc(StatutPrix statut);
}
