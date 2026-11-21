package tn.smartbuy.budget;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.smartbuy.alerte.AlerteService;
import tn.smartbuy.budget.BudgetDtos.OperationDto;
import tn.smartbuy.budget.BudgetDtos.OperationRequete;
import tn.smartbuy.commun.AccesInterditException;
import tn.smartbuy.commun.Montants;
import tn.smartbuy.commun.RequeteInvalideException;
import tn.smartbuy.commun.RessourceIntrouvableException;
import tn.smartbuy.prix.CatalogueService;
import tn.smartbuy.prix.Magasin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Revenus et dépenses (BUD-03 à BUD-07). */
@Service
public class OperationService {

    private final OperationRepository operations;
    private final CategorieService categorieService;
    private final CatalogueService catalogue;
    private final AlerteService alerteService;

    public OperationService(OperationRepository operations, CategorieService categorieService,
                            CatalogueService catalogue, AlerteService alerteService) {
        this.operations = operations;
        this.categorieService = categorieService;
        this.catalogue = catalogue;
        this.alerteService = alerteService;
    }

    /** BUD-06 : historique filtré par mois, type ou catégorie (filtres facultatifs), plus récentes d'abord. */
    @Transactional(readOnly = true)
    public List<OperationDto> lister(Long utilisateurId, YearMonth mois, TypeOperation type, Long categorieId) {
        List<Operation> liste = mois == null
                ? operations.findByUtilisateurIdOrderByDateOperationDescIdDesc(utilisateurId)
                : operations.findByUtilisateurIdAndDateOperationBetweenOrderByDateOperationDescIdDesc(
                        utilisateurId, mois.atDay(1), mois.atEndOfMonth());
        return liste.stream()
                .filter(o -> type == null || o.getType() == type)
                .filter(o -> categorieId == null || o.getCategorie().getId().equals(categorieId))
                .map(OperationDto::depuis)
                .toList();
    }

    /** BUD-03 / BUD-04 (+ BUD-11 : contrôle des alertes après une dépense). */
    @Transactional
    public OperationDto creer(Long utilisateurId, OperationRequete requete) {
        Operation o = new Operation();
        o.setUtilisateurId(utilisateurId);
        remplir(o, utilisateurId, requete);
        operations.save(o);
        verifierAlertes(o);
        return OperationDto.depuis(o);
    }

    /** BUD-05 : modifier (l'économie éventuelle d'un achat de liste est conservée). */
    @Transactional
    public OperationDto modifier(Long utilisateurId, Long id, OperationRequete requete) {
        Operation o = chargerMienne(utilisateurId, id);
        remplir(o, utilisateurId, requete);
        verifierAlertes(o);
        return OperationDto.depuis(o);
    }

    /** BUD-05 : supprimer. */
    @Transactional
    public void supprimer(Long utilisateurId, Long id) {
        operations.delete(chargerMienne(utilisateurId, id));
    }

    /**
     * LNK-02 : dépense créée à partir d'une liste de courses achetée.
     * Passe par les mêmes contrôles d'alerte qu'une dépense saisie à la main.
     */
    @Transactional
    public OperationDto creerDepenseAchat(Long utilisateurId, Categorie categorie, BigDecimal montant,
                                         Magasin magasin, String note, BigDecimal economie) {
        Operation o = new Operation();
        o.setUtilisateurId(utilisateurId);
        o.setType(TypeOperation.DEPENSE);
        o.setCategorie(categorie);
        o.setMontant(Montants.arrondir(montant));
        o.setDateOperation(LocalDate.now());
        o.setMagasin(magasin);
        o.setNote(note.length() > 255 ? note.substring(0, 255) : note);
        o.setRecurrente(false);
        o.setEconomie(Montants.arrondir(economie));
        operations.save(o);
        verifierAlertes(o);
        return OperationDto.depuis(o);
    }

    /**
     * BUD-07 : recopie dans {@code moisCible} les opérations récurrentes du mois précédent.
     * Le jour est conservé (31 -> 30 ou 28 si le mois est plus court). Une copie déjà présente
     * (même catégorie, même montant, même date) n'est pas recréée : la tâche peut être relancée sans doublon.
     *
     * @return le nombre d'opérations créées
     */
    @Transactional
    public int copierRecurrentes(YearMonth moisCible) {
        YearMonth moisSource = moisCible.minusMonths(1);
        List<Operation> recurrentes = operations.findByRecurrenteAndDateOperationBetween(
                true, moisSource.atDay(1), moisSource.atEndOfMonth());
        int creees = 0;
        for (Operation source : recurrentes) {
            int jour = Math.min(source.getDateOperation().getDayOfMonth(), moisCible.lengthOfMonth());
            LocalDate date = moisCible.atDay(jour);
            boolean dejaCopiee = operations.existsByUtilisateurIdAndCategorieIdAndMontantAndDateOperation(
                    source.getUtilisateurId(), source.getCategorie().getId(), source.getMontant(), date);
            if (dejaCopiee) {
                continue;
            }
            Operation copie = new Operation();
            copie.setUtilisateurId(source.getUtilisateurId());
            copie.setType(source.getType());
            copie.setCategorie(source.getCategorie());
            copie.setMontant(source.getMontant());
            copie.setDateOperation(date);
            copie.setNote(source.getNote());
            copie.setMagasin(source.getMagasin());
            copie.setRecurrente(true); // la copie sera recopiée le mois suivant
            operations.save(copie);
            creees++;
        }
        return creees;
    }

    private Operation chargerMienne(Long utilisateurId, Long id) {
        Operation o = operations.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Opération introuvable"));
        if (!o.getUtilisateurId().equals(utilisateurId)) {
            throw new AccesInterditException("Cette opération ne vous appartient pas");
        }
        return o;
    }

    private void remplir(Operation o, Long utilisateurId, OperationRequete r) {
        Categorie categorie = categorieService.chargerVisible(utilisateurId, r.categorieId());
        if (categorie.getType() != r.type()) {
            throw new RequeteInvalideException("La catégorie « " + categorie.getNom()
                    + " » est une catégorie de type " + categorie.getType() + ", pas " + r.type());
        }
        o.setType(r.type());
        o.setCategorie(categorie);
        o.setMontant(Montants.depuisDouble(r.montant()));
        o.setDateOperation(r.dateOperation());
        o.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        o.setMagasin(r.magasinId() == null ? null : catalogue.chargerMagasin(r.magasinId()));
        o.setRecurrente(Boolean.TRUE.equals(r.recurrente()));
    }

    private void verifierAlertes(Operation o) {
        if (o.getType() == TypeOperation.DEPENSE) {
            alerteService.verifierBudgets(o.getUtilisateurId(), YearMonth.from(o.getDateOperation()),
                    o.getCategorie().getId());
        }
    }
}
