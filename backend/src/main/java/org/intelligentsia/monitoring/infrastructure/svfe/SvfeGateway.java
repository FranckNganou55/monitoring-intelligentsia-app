package org.intelligentsia.monitoring.infrastructure.svfe;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Currency;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.InstitutionCurrency;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.CurrencyRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionCurrencyRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.Atm;
import org.intelligentsia.monitoring.domain.monitoring.atm.entity.AtmSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmRepository;
import org.intelligentsia.monitoring.domain.monitoring.atm.repository.AtmSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.CardOverLimit;
import org.intelligentsia.monitoring.domain.monitoring.fleet.entity.FleetAccount;
import org.intelligentsia.monitoring.domain.monitoring.fleet.repository.CardOverLimitRepository;
import org.intelligentsia.monitoring.domain.monitoring.fleet.repository.FleetAccountRepository;
import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudDetail;
import org.intelligentsia.monitoring.domain.monitoring.fraud.entity.FraudRate;
import org.intelligentsia.monitoring.domain.monitoring.fraud.repository.FraudDetailRepository;
import org.intelligentsia.monitoring.domain.monitoring.fraud.repository.FraudRateRepository;
import org.intelligentsia.monitoring.domain.monitoring.host.entity.HostSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.host.repository.HostSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.network.entity.NetworkSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.network.repository.NetworkSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.pos.entity.PosTelecollectSnapshot;
import org.intelligentsia.monitoring.domain.monitoring.pos.repository.PosTelecollectSnapshotRepository;
import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionError;
import org.intelligentsia.monitoring.domain.monitoring.transaction.entity.TransactionRate;
import org.intelligentsia.monitoring.domain.monitoring.transaction.repository.TransactionErrorRepository;
import org.intelligentsia.monitoring.domain.monitoring.transaction.repository.TransactionRateRepository;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsAtm;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsCardLimit;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsFraud;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.CmsRule;
import org.intelligentsia.monitoring.infrastructure.svfe.entity.SSAtm;
import org.intelligentsia.monitoring.infrastructure.svfe.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SvfeGateway {

    @Value("${app.svfe.enabled:false}")
    private boolean svfeEnabled;

    // SVFE repositories — Optional because @ConditionalOnProperty may exclude them
    private final Optional<CmsAtmRepository>          cmsAtmRepo;
    private final Optional<CmsNetworkRepository>      cmsNetworkRepo;
    private final Optional<CmsHostRepository>         cmsHostRepo;
    private final Optional<CmsTransactionRepository>  cmsTransactionRepo;
    private final Optional<CmsFraudRepository>        cmsFraudRepo;
    private final Optional<CmsRuleRepository>         cmsRuleRepo;
    private final Optional<CmsAccountRepository>      cmsAccountRepo;
    private final Optional<CmsCardLimitRepository>    cmsCardLimitRepo;
    private final Optional<CmsPosRepository>          cmsPosRepo;
    private final Optional<CmsTeleCollectRepository>  cmsTeleCollectRepo;

    // Primary (PostgreSQL) repositories
    private final AtmRepository                    atmRepo;
    private final AtmSnapshotRepository            atmSnapshotRepo;
    private final NetworkSnapshotRepository        networkSnapshotRepo;
    private final HostSnapshotRepository           hostSnapshotRepo;
    private final TransactionRateRepository        txnRateRepo;
    private final TransactionErrorRepository       txnErrorRepo;
    private final FraudDetailRepository            fraudDetailRepo;
    private final FraudRateRepository              fraudRateRepo;
    private final FleetAccountRepository           fleetAccountRepo;
    private final CardOverLimitRepository          cardOverLimitRepo;
    private final PosTelecollectSnapshotRepository posTelecollectRepo;
    private final InstitutionRepository            institutionRepo;
    private final CurrencyRepository               currencyRepo;
    private final InstitutionCurrencyRepository    institutionCurrencyRepo;

    // -------------------------------------------------------------------------
    // ATM sync — reads SSAtm rows, groups by terminalId into 4-cassette CmsAtm
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncAtms() {
        if (!svfeEnabled || cmsAtmRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncAtms ignoré.");
            return;
        }
        log.info("Synchronisation ATMs depuis SVFE Oracle...");

        List<SSAtm> rows = cmsAtmRepo.get().findATMs();
        Map<Long, List<SSAtm>> byTerminal = rows.stream()
                .collect(Collectors.groupingBy(SSAtm::getTerminalId));

        // Build in-memory CmsAtm (one per terminal, 4 cassettes)
        List<CmsAtm> atms = byTerminal.values().stream()
                .map(this::buildCmsAtm)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Institution> institutionsBySvfeId = buildInstitutionBySvfeId();
        Map<Long, Currency>      currenciesByNumeric  = buildCurrencyByIsoNumericLong();
        Map<String, Atm>         knownAtms            = atmRepo.findAll().stream()
                .collect(Collectors.toMap(Atm::getTerminalId, Function.identity(), (a, b) -> a));

        Instant now = Instant.now();
        List<AtmSnapshot> snapshots = new ArrayList<>();

        for (CmsAtm src : atms) {
            String terminalIdStr = String.valueOf(src.getTerminalId());

            // Resolve or auto-create institution from SVFE BAN_IDEN
            Institution inst = institutionsBySvfeId.computeIfAbsent(src.getInstitution(), svfeId -> {
                Institution newInst = new Institution();
                newInst.setCode(svfeId);
                newInst.setName(svfeId);
                newInst.setSvfeId(svfeId);
                Institution saved = institutionRepo.save(newInst);
                log.info("Institution auto-créée depuis SVFE: '{}'", svfeId);
                return saved;
            });

            Atm atm = knownAtms.computeIfAbsent(terminalIdStr, tid -> {
                Atm a = new Atm();
                a.setTerminalId(tid);
                a.setInstitution(inst);
                a.setAddress(src.getAdress());
                a.setCity(src.getVille());
                a.setSvfePid(src.getPid());
                return atmRepo.save(a);
            });

            AtmSnapshot snap = new AtmSnapshot();
            snap.setAtm(atm);
            snap.setSnapshotTime(now);
            snap.setStatusId(src.getStatusId() != null ? String.valueOf(src.getStatusId()) : null);
            snap.setStatusDesc(src.getStatusDesc());
            snap.setAllOk(isStatusOk(src.getStatusId()));

            Currency cur1 = src.getCurrency1() != null ? currenciesByNumeric.get(src.getCurrency1()) : null;
            Currency cur2 = src.getCurrency2() != null ? currenciesByNumeric.get(src.getCurrency2()) : null;
            Currency cur3 = src.getCurrency3() != null ? currenciesByNumeric.get(src.getCurrency3()) : null;
            Currency cur4 = src.getCurrency4() != null ? currenciesByNumeric.get(src.getCurrency4()) : null;

            snap.setCurrency1(cur1);
            snap.setNote1(toInt(src.getNote1()));
            snap.setQuantity1(toInt(src.getNbre1()));

            snap.setCurrency2(cur2);
            snap.setNote2(toInt(src.getNote2()));
            snap.setQuantity2(toInt(src.getNbre2()));

            snap.setCurrency3(cur3);
            snap.setNote3(toInt(src.getNote3()));
            snap.setQuantity3(toInt(src.getNbre3()));

            snap.setCurrency4(cur4);
            snap.setNote4(toInt(src.getNote4()));
            snap.setQuantity4(toInt(src.getNbre4()));

            // Threshold check for cassette 1 (primary currency)
            if (atm.getThreshold1() != null && src.getNote1() != null && src.getNbre1() != null) {
                BigDecimal amount = BigDecimal.valueOf(src.getNote1() * src.getNbre1());
                snap.setThreshold1Exceeded(amount.compareTo(atm.getThreshold1()) < 0);
            }
            // Threshold check for cassette 2
            if (atm.getThreshold2() != null && src.getNote2() != null && src.getNbre2() != null) {
                BigDecimal amount = BigDecimal.valueOf(src.getNote2() * src.getNbre2());
                snap.setThreshold2Exceeded(amount.compareTo(atm.getThreshold2()) < 0);
            }

            snapshots.add(snap);
        }

        atmSnapshotRepo.saveAll(snapshots);
        log.info("ATM sync terminé — {} snapshots enregistrés.", snapshots.size());
    }

    // -------------------------------------------------------------------------
    // Network sync
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncNetworks() {
        if (!svfeEnabled || cmsNetworkRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncNetworks ignoré.");
            return;
        }
        log.info("Synchronisation réseaux depuis SVFE Oracle...");

        Instant now = Instant.now();
        List<NetworkSnapshot> snaps = cmsNetworkRepo.get().findNetwok().stream()
                .map(src -> {
                    NetworkSnapshot s = new NetworkSnapshot();
                    s.setNetworkCode(src.getPid());
                    s.setDescription(src.getDescription());
                    String status = opStatToStatus(src.getOpStat());
                    s.setOperatingStatus(status);
                    s.setProcessingStatus(status);
                    s.setSnapshotTime(now);
                    return s;
                })
                .collect(Collectors.toList());

        networkSnapshotRepo.saveAll(snaps);
        log.info("Network sync terminé — {} snapshots.", snaps.size());
    }

    // -------------------------------------------------------------------------
    // Host sync
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncHosts() {
        if (!svfeEnabled || cmsHostRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncHosts ignoré.");
            return;
        }
        log.info("Synchronisation hôtes depuis SVFE Oracle...");

        Instant now = Instant.now();
        var sourceHosts = cmsHostRepo.get().findHost();
        if (sourceHosts.isEmpty()) {
            log.warn("HOST_QUERY n'a renvoyé aucun hôte. Vérifiez les INT_ITY_CODE et INT_IDEN dans INTERFACES côté SVFE.");
        }
        List<HostSnapshot> snaps = sourceHosts.stream()
                .map(src -> {
                    HostSnapshot s = new HostSnapshot();
                    s.setHostname(src.getPid());
                    s.setDescription(src.getDescription());
                    String status = opStatToStatus(src.getOpStat());
                    s.setOperatingStatus(status);
                    s.setProcessingStatus(status);
                    s.setSnapshotTime(now);
                    return s;
                })
                .collect(Collectors.toList());

        hostSnapshotRepo.saveAll(snaps);
        log.info("Host sync terminé — {} snapshots.", snaps.size());
    }

    // -------------------------------------------------------------------------
    // Transaction sync — groups per-row counts into rates and errors
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncTransactions() {
        if (!svfeEnabled || cmsTransactionRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncTransactions ignoré.");
            return;
        }
        log.info("Synchronisation transactions depuis SVFE Oracle...");

        var allTxns = cmsTransactionRepo.get().findTransaction(new java.util.Date(), 15);
        Instant now = Instant.now();

        // Group by (network, acquirer, issuer) → TransactionRate
        record RateKey(String network, String acquirer, String issuer) {}
        Map<RateKey, int[]> rateAcc = new LinkedHashMap<>();
        Map<String, Map<String, int[]>> errorAcc = new LinkedHashMap<>(); // network → responseCode → [count, idx]

        for (var t : allTxns) {
            int count = t.getNbre() != null ? t.getNbre() : 0;
            RateKey key = new RateKey(t.getNetwork(), t.getAcquirer(), t.getIssuer());
            int[] acc = rateAcc.computeIfAbsent(key, k -> new int[]{0, 0});
            if (t.isSuccess()) {
                acc[0] += count;
            } else {
                acc[1] += count;
                // Accumulate errors per (network, responseCode)
                errorAcc
                    .computeIfAbsent(t.getNetwork(), k -> new LinkedHashMap<>())
                    .merge(t.getResponseCode(), new int[]{count, 0}, (a, b) -> new int[]{a[0] + b[0], 0});
            }
        }

        List<TransactionRate> rates = new ArrayList<>();
        for (var entry : rateAcc.entrySet()) {
            RateKey k = entry.getKey();
            int success = entry.getValue()[0];
            int failure = entry.getValue()[1];
            int total = success + failure;
            TransactionRate r = new TransactionRate();
            r.setNetwork(k.network());
            r.setAcquirer(k.acquirer());
            r.setIssuer(k.issuer());
            r.setSuccessCount(success);
            r.setFailureCount(failure);
            if (total > 0) {
                r.setSuccessRate(BigDecimal.valueOf(success * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
                r.setFailureRate(BigDecimal.valueOf(failure * 100.0 / total).setScale(2, RoundingMode.HALF_UP));
            }
            r.setSnapshotTime(now);
            rates.add(r);
        }
        txnRateRepo.saveAll(rates);

        // Build transaction errors (top errors by network)
        List<TransactionError> errors = new ArrayList<>();
        errorAcc.forEach((network, codeMap) ->
            codeMap.forEach((code, counts) -> {
                // Find responseDesc from the original data
                String desc = allTxns.stream()
                        .filter(t -> network.equals(t.getNetwork()) && code.equals(t.getResponseCode()))
                        .map(t -> t.getResponseDesc())
                        .findFirst().orElse(null);
                TransactionError e = new TransactionError();
                e.setNetwork(network);
                e.setResponseCode(code);
                e.setResponseDesc(desc);
                e.setCount(counts[0]);
                e.setSnapshotTime(now);
                errors.add(e);
            })
        );
        txnErrorRepo.saveAll(errors);

        log.info("Transaction sync terminé — {} taux, {} erreurs.", rates.size(), errors.size());
    }

    // -------------------------------------------------------------------------
    // Fraud sync — per-institution, per-bin, per-rule
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncFraud() {
        if (!svfeEnabled || cmsFraudRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncFraud ignoré.");
            return;
        }
        log.info("Synchronisation fraude depuis SVFE Oracle...");

        List<CmsFraud> allFrauds = cmsFraudRepo.get().findFraud();
        Map<String, Institution> institutionsBySvfeId = buildInstitutionBySvfeId();
        Map<String, String> ruleDescriptions = buildRuleDescriptions();
        Instant now = Instant.now();

        Map<String, FraudDetail> detailsBySource = new LinkedHashMap<>();
        // rateKey = institutionId_bin
        Map<String, FraudRate> rateMap = new LinkedHashMap<>();

        for (Institution institution : institutionsBySvfeId.values()) {
            List<CmsFraud> forInst = allFrauds.stream()
                    .filter(f -> f.isBelongToInstitution(institution.getSvfeId()))
                    .collect(Collectors.toList());

            for (CmsFraud fraud : forInst) {
                boolean suspect = fraud.isSuspect();

                // Split the CSV rules field (e.g. "12,45,67")
                List<String> ruleIds = fraud.getRules() != null && !fraud.getRules().isBlank()
                        ? Arrays.asList(fraud.getRules().split(","))
                        : List.of("?");

                for (String ruleId : ruleIds.stream().map(String::trim).filter(id -> !id.isBlank()).distinct().toList()) {
                    String ruleIdTrimmed = ruleId.trim();
                    String ruleDesc = ruleDescriptions.getOrDefault(ruleIdTrimmed, ruleIdTrimmed);
                    String detailKey = institution.getId() + "|" + fraud.getBin() + "|" + ruleIdTrimmed;
                    FraudDetail d = detailsBySource.computeIfAbsent(detailKey, key -> new FraudDetail());
                    d.setInstitution(institution);
                    d.setBin(fraud.getBin());
                    d.setRuleCode(ruleIdTrimmed);
                    d.setRuleDesc(ruleDesc);
                    d.setDescription(fraud.getDescription());
                    d.setSuspectCount(suspect ? 1 : 0);
                    d.setFraudCount(suspect ? 0 : 1);
                    d.setSnapshotTime(now);
                }

                // Aggregate per institution+bin for FraudRate
                String rateKey = institution.getId() + "_" + fraud.getBin();
                rateMap.compute(rateKey, (k, r) -> {
                    if (r == null) {
                        r = new FraudRate();
                        r.setInstitution(institution);
                        r.setBin(fraud.getBin());
                        r.setSuspectCount(0);
                        r.setFraudCount(0);
                        r.setSnapshotTime(now);
                    }
                    if (suspect) r.setSuspectCount(r.getSuspectCount() + 1);
                    else         r.setFraudCount(r.getFraudCount() + 1);
                    return r;
                });
            }
        }

        fraudDetailRepo.saveAll(detailsBySource.values());
        fraudRateRepo.saveAll(rateMap.values());
        log.info("Fraud sync terminé — {} détails, {} taux.", detailsBySource.size(), rateMap.size());
    }

    // -------------------------------------------------------------------------
    // Fleet sync — accounts + cards over limit
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncFleet() {
        if (!svfeEnabled || cmsAccountRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncFleet ignoré.");
            return;
        }
        log.info("Synchronisation flotte depuis SVFE Oracle...");

        Map<String, Institution> institutionsBySvfeId = buildInstitutionBySvfeId();
        Map<String, Currency>    currenciesByNumericStr = buildCurrencyByIsoNumericStr();
        Instant now = Instant.now();

        // --- Fleet accounts ---
        List<FleetAccount> accounts = new ArrayList<>();
        for (var src : cmsAccountRepo.get().findSoldeFloat()) {
            Institution inst = institutionsBySvfeId.get(src.getInstitution());
            Currency cur = currenciesByNumericStr.get(src.getCurrency());
            if (inst == null || cur == null) continue;

            // Oracle treats '' as NULL; fall back to a synthetic key: INSTCODE-CURRCODE
            String accountNumber = (src.getAccount() != null && !src.getAccount().isBlank())
                    ? src.getAccount()
                    : inst.getCode() + "-" + cur.getIsoAlpha();

            BigDecimal currentBalance = src.getBalance() != null
                    ? BigDecimal.valueOf(src.getBalance()) : BigDecimal.ZERO;

            // Retrieve previous balance from last saved snapshot
            BigDecimal previousBalance = fleetAccountRepo
                    .findFirstByInstitutionIdAndCurrencyIdOrderBySnapshotTimeDesc(inst.getId(), cur.getId())
                    .map(FleetAccount::getBalance)
                    .orElse(null);

            FleetAccount fa = new FleetAccount();
            fa.setInstitution(inst);
            fa.setAccountNumber(accountNumber);
            fa.setCurrency(cur);
            fa.setBalance(currentBalance);
            fa.setPreviousBalance(previousBalance);

            if (previousBalance != null) {
                BigDecimal delta = currentBalance.subtract(previousBalance);
                fa.setDelta1(delta);
                // Get delta thresholds from InstitutionCurrency config
                institutionCurrencyRepo.findByInstitutionId(inst.getId()).stream()
                        .filter(ic -> ic.getCurrency().getId().equals(cur.getId()))
                        .findFirst()
                        .ifPresent(ic -> {
                            if (ic.getAtmThreshold() != null) {
                                fa.setDelta1Exceeded(delta.abs().compareTo(ic.getAtmThreshold()) > 0);
                            }
                        });
            }
            fa.setSnapshotTime(now);
            accounts.add(fa);
        }
        fleetAccountRepo.saveAll(accounts);

        // --- Cards over limit ---
        syncCardLimits(institutionsBySvfeId, currenciesByNumericStr, now);

        log.info("Fleet sync terminé — {} comptes flotte.", accounts.size());
    }

    private void syncCardLimits(Map<String, Institution> institutionsBySvfeId,
                                Map<String, Currency> currenciesByNumericStr,
                                Instant now) {
        if (cmsCardLimitRepo.isEmpty()) return;

        List<CardOverLimit> overLimits = new ArrayList<>();
        for (Institution inst : institutionsBySvfeId.values()) {
            List<InstitutionCurrency> configs = institutionCurrencyRepo.findByInstitutionId(inst.getId());
            for (InstitutionCurrency ic : configs) {
                if (ic.getCardPpLimit() == null) continue;
                Currency cur = ic.getCurrency();
                String isoNumeric = cur.getIsoNumeric();
                double limit = ic.getCardPpLimit().doubleValue();

                List<CmsCardLimit> cards = cmsCardLimitRepo.get().findCardLimit(inst.getSvfeId(), isoNumeric, limit);
                for (var card : cards) {
                    CardOverLimit col = new CardOverLimit();
                    col.setInstitution(inst);
                    col.setCardNumber(card.getCardNumber());
                    col.setCurrency(cur);
                    col.setLimitAmount(ic.getCardPpLimit());
                    col.setCurrentAmount(card.getBalance() != null
                            ? BigDecimal.valueOf(card.getBalance()) : BigDecimal.ZERO);
                    col.setExceeded(true); // already filtered by findCardLimit query
                    col.setSnapshotTime(now);
                    overLimits.add(col);
                }
            }
        }
        cardOverLimitRepo.saveAll(overLimits);
        log.info("Cards over limit sync — {} cartes.", overLimits.size());
    }

    // -------------------------------------------------------------------------
    // Telecollect POS sync
    // -------------------------------------------------------------------------

    @Transactional("transactionManager")
    public void syncTeleCollect() {
        if (!svfeEnabled || cmsTeleCollectRepo.isEmpty()) {
            log.debug("SVFE désactivé — syncTeleCollect ignoré.");
            return;
        }
        log.info("Synchronisation télécollecte POS depuis SVFE Oracle...");

        Map<String, Institution> institutionsBySvfeId = buildInstitutionBySvfeId();
        // Build set of terminal IDs pending telecollect
        Set<String> pendingTerminals = cmsTeleCollectRepo.get().teleCollectPos().stream()
                .map(t -> t.getTerminalID())
                .collect(Collectors.toSet());

        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        List<PosTelecollectSnapshot> snaps = new ArrayList<>();

        // For each POS known in SVFE, determine telecollect status
        if (cmsPosRepo.isPresent()) {
            for (var pos : cmsPosRepo.get().findAll()) {
                Institution inst = institutionsBySvfeId.get(pos.getInstitution());
                if (inst == null) continue;

                boolean isPending = pendingTerminals.contains(pos.getTerminalID());
                PosTelecollectSnapshot s = new PosTelecollectSnapshot();
                s.setInstitution(inst);
                s.setTerminalId(pos.getTerminalID());
                s.setCollectDate(today);
                s.setStatus(isPending ? "NON_OK" : "OK");
                s.setSnapshotTime(now);
                snaps.add(s);
            }
        }

        posTelecollectRepo.saveAll(snaps);
        log.info("Télécollecte sync terminé — {} snapshots ({} en attente).",
                snaps.size(), pendingTerminals.size());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CmsAtm buildCmsAtm(List<SSAtm> rows) {
        if (rows == null || rows.isEmpty()) return null;
        SSAtm first = rows.get(0);
        CmsAtm atm = new CmsAtm();
        atm.setPid(first.getPid());
        atm.setInstitution(first.getInstitution());
        atm.setTerminalId(String.valueOf(first.getTerminalId()));
        atm.setAdress(first.getAdress());
        atm.setVille(first.getVille());
        atm.setStatusId(first.getStatusId());
        atm.setStatusDesc(first.getStatusDesc());

        for (SSAtm row : rows) {
            int slot = row.getCassette() != null ? row.getCassette().intValue() : 0;
            switch (slot) {
                case 1 -> { atm.setCurrency1(row.getCurrency()); atm.setNote1(row.getNote()); atm.setNbre1(row.getNombreRest()); }
                case 2 -> { atm.setCurrency2(row.getCurrency()); atm.setNote2(row.getNote()); atm.setNbre2(row.getNombreRest()); }
                case 3 -> { atm.setCurrency3(row.getCurrency()); atm.setNote3(row.getNote()); atm.setNbre3(row.getNombreRest()); }
                case 4 -> { atm.setCurrency4(row.getCurrency()); atm.setNote4(row.getNote()); atm.setNbre4(row.getNombreRest()); }
                default -> log.warn("SSAtm pid={} terminalId={} — cassette inattendue: {}", row.getPid(), row.getTerminalId(), slot);
            }
        }
        return atm;
    }

    private Map<String, Institution> buildInstitutionBySvfeId() {
        Map<String, Institution> institutions = new HashMap<>();
        List<Institution> allInstitutions = institutionRepo.findAll();
        for (Institution institution : allInstitutions) {
            if (institution.getCode() != null && !institution.getCode().isBlank()) {
                institutions.putIfAbsent(institution.getCode().trim(), institution);
            }
        }
        // Prefer explicit source IDs when they differ from the locally assigned institution code.
        for (Institution institution : allInstitutions) {
            if (institution.getSvfeId() != null && !institution.getSvfeId().isBlank()) {
                institutions.put(institution.getSvfeId().trim(), institution);
            }
        }
        return institutions;
    }

    private Map<Long, Currency> buildCurrencyByIsoNumericLong() {
        return currencyRepo.findAll().stream()
                .filter(c -> c.getIsoNumeric() != null)
                .collect(Collectors.toMap(
                        c -> Long.parseLong(c.getIsoNumeric()),
                        Function.identity(),
                        (a, b) -> a));
    }

    private Map<String, Currency> buildCurrencyByIsoNumericStr() {
        return currencyRepo.findAll().stream()
                .filter(c -> c.getIsoNumeric() != null)
                .collect(Collectors.toMap(Currency::getIsoNumeric, Function.identity(), (a, b) -> a));
    }

    private Map<String, String> buildRuleDescriptions() {
        if (cmsRuleRepo.isEmpty()) return Map.of();
        return cmsRuleRepo.get().findAllRules().stream()
                .filter(r -> r.getRuleId() != null)
                .collect(Collectors.toMap(CmsRule::getRuleId, r ->
                        r.getRuleDescription() != null ? r.getRuleDescription() : r.getRuleId(),
                        (a, b) -> a));
    }

    private boolean isStatusOk(Long statusId) {
        return statusId != null && statusId == 0L;
    }

    private String opStatToStatus(Character opStat) {
        if (opStat == null) return "UNKNOWN";
        char c = Character.toUpperCase(opStat);
        // Oracle DECODE returns 'U'=Up, 'D'=Down, 'S'=Stopped; also handle legacy 'Y'/'N'/'1'/'0'
        if (c == 'U' || c == 'Y' || c == '1') return "OK";
        if (c == 'D' || c == 'N' || c == '0' || c == 'S') return "NOK";
        return "UNKNOWN"; // 'S'=Stopped or anything unrecognised
    }

    private Integer toInt(Double d) {
        return d != null ? d.intValue() : null;
    }
}
