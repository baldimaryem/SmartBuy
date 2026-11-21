package tn.smartbuy.prix;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.auth.Utilisateur;
import tn.smartbuy.auth.UtilisateurRepository;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.prix.PrixDtos.HistoriquePrixDto;
import tn.smartbuy.prix.PrixDtos.PrixDto;
import tn.smartbuy.prix.PrixDtos.PrixMagasinDto;
import tn.smartbuy.prix.PrixDtos.PrixRequete;
import tn.smartbuy.securite.UtilisateurConnecte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Prix : comparaison par magasin, historique, propositions des utilisateurs et validation. */
@Service
public class PrixService {

    private final PrixRepository prix;
    private final CatalogueService catalogue;
    private final UtilisateurRepository utilisateurs;

    public PrixService(PrixRepository prix, CatalogueService catalogue, UtilisateurRepository utilisateurs) {
        this.prix = prix;
        this.catalogue = catalogue;
        this.utilisateurs = utilisateurs;
    }

    /** PRX-04 : dernier prix VALIDE dans chaque magasin, du moins cher au plus cher, le meilleur marqué. */
    @Transactional(readOnly = true)
    public List<PrixMagasinDto> prixParMagasin(Long produitId) {
        catalogue.chargerProduit(produitId);
        List<Prix> tries = PrixActuels.pourUnProduitTries(
                prix.findByProduitIdAndStatutOrderByDateReleveAscIdAsc(produitId, StatutPrix.VALIDE));
        if (tries.isEmpty()) {
            return List.of();
        }
        BigDecimal minimum = tries.get(0).getMontant();
        return tries.stream()
                .map(p -> new PrixMagasinDto(p.getId(), p.getMagasin().getId(), p.getMagasin().getNom(),
                        p.getMagasin().getEnseigne(), Montants.versDouble(p.getMontant()), p.getDateReleve(),
                        // en cas d'égalité, tous les magasins au prix minimum sont « meilleurs »
                        p.getMontant().compareTo(minimum) == 0, p.getSource(), p.getMagasin().isEnLigne()))
                .toList();
    }

    /** PRX-05 : tous les relevés validés, triés par date. */
    @Transactional(readOnly = true)
    public List<HistoriquePrixDto> historique(Long produitId) {
        catalogue.chargerProduit(produitId);
        return prix.findByProduitIdAndStatutOrderByDateReleveAscIdAsc(produitId, StatutPrix.VALIDE).stream()
                .map(p -> new HistoriquePrixDto(p.getMagasin().getNom(), Montants.versDouble(p.getMontant()),
                        p.getDateReleve()))
                .toList();
    }

    /**
     * PRX-07 : un utilisateur propose un prix (EN_ATTENTE).
     * ADM-04 : un administrateur saisit directement un prix VALIDE.
     */
    @Transactional
    public PrixDto proposer(UtilisateurConnecte auteur, PrixRequete requete) {
        Utilisateur utilisateur = utilisateurs.findById(auteur.id())
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable"));
        Prix p = new Prix();
        p.setProduit(catalogue.chargerProduit(requete.produitId()));
        p.setMagasin(catalogue.chargerMagasin(requete.magasinId()));
        p.setMontant(Montants.depuisDouble(requete.montant()));
        p.setDateReleve(requete.dateReleve() == null ? LocalDate.now() : requete.dateReleve());
        if (p.getDateReleve().isAfter(LocalDate.now())) {
            throw new RequeteInvalideException("La date du relevé ne peut pas être dans le futur");
        }
        p.setProposePar(utilisateur);
        if (auteur.estAdmin()) {
            p.setSource(SourcePrix.ADMIN);
            p.setStatut(StatutPrix.VALIDE);
        } else {
            p.setSource(SourcePrix.UTILISATEUR);
            p.setStatut(StatutPrix.EN_ATTENTE);
        }
        return PrixDto.depuis(prix.save(p));
    }

    /** PRX-08 : liste des propositions à traiter. */
    @Transactional(readOnly = true)
    public List<PrixDto> enAttente() {
        return prix.findByStatutOrderByDateReleveAscIdAsc(StatutPrix.EN_ATTENTE).stream()
                .map(PrixDto::depuis).toList();
    }

    /** PRX-08 */
    @Transactional
    public PrixDto valider(Long id) {
        return changerStatut(id, StatutPrix.VALIDE);
    }

    /** PRX-08 */
    @Transactional
    public PrixDto rejeter(Long id) {
        return changerStatut(id, StatutPrix.REJETE);
    }

    /** Prix actuels de plusieurs produits : produitId -> (magasinId -> prix). Utilisé par les listes de courses. */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, Prix>> prixActuels(Collection<Long> produitIds) {
        if (produitIds.isEmpty()) {
            return Map.of();
        }
        return PrixActuels.parProduitEtMagasin(prix.findByProduitIdInAndStatut(produitIds, StatutPrix.VALIDE));
    }

    /** Prix actuels de tout le catalogue (pour le conseil « meilleur magasin »). */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, Prix>> tousLesPrixActuels() {
        return PrixActuels.parProduitEtMagasin(prix.findByStatut(StatutPrix.VALIDE));
    }

    private PrixDto changerStatut(Long id, StatutPrix nouveau) {
        Prix p = prix.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Prix introuvable"));
        if (p.getStatut() != StatutPrix.EN_ATTENTE) {
            throw new RequeteInvalideException("Ce prix a déjà été traité (statut " + p.getStatut() + ")");
        }
        p.setStatut(nouveau);
        return PrixDto.depuis(p);
    }
}
