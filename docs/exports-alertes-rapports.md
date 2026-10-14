# Exports des alertes et rapports journaliers

Les générateurs communs produisent des documents A4 portrait en PDF, Word et Excel. Les titres de section sont différenciés, les tableaux alternent leurs fonds et les fiches d’alerte portent un bandeau selon leur risque : rouge (critique), orange (moyen), doré (moins critique), bleu (typique). Les dates et les statuts sont présentés en français.

## Archives

Le sélecteur « Alertes à exporter » propose toutes les alertes (actives et archivées) ou les alertes archivées uniquement. Les filtres de recherche, catégorie, institution et date restent appliqués, sans limitation à la page affichée.

`GET /api/monitoring/alerts/archives/export?format=PDF&includeActive=true`

Formats : `PDF`, `WORD`, `EXCEL`. Sans `includeActive`, le serveur exporte seulement les alertes archivées. Les données supprimées ne sont jamais incluses. L’institution de l’utilisateur reste imposée pour les comptes institutionnels.

## Rapports journaliers

Les champs « Du » et « Au » délimitent l’aperçu, l’historique des rapports et les exports. Les heures s’appliquent au premier et au dernier jour. Sans heure, les journées sont incluses entièrement; l’heure de fin inclut toute la minute choisie. L’aperçu affiche au plus 100 alertes et indique le total; l’export inclut toutes les alertes correspondantes.

Pour une seule journée, la génération et l’historique existants sont conservés. Pour plusieurs jours, le document est téléchargé directement et ne remplace aucun rapport journalier enregistré.

`GET /api/fraud/reports/daily/export-period?fromDate=2026-09-28&toDate=2026-10-12&format=PDF`

Paramètres facultatifs : `comment`, `fromTime`, `untilTime`. Les dates invalides sont rejetées. L’historique accepte `fromDate` et `toDate`, bornes incluses, en plus de la pagination existante.

Limitation préexistante : les rapports enregistrés ne possèdent pas de rattachement institutionnel. Leur génération, l’export sur période et leur téléchargement restent indisponibles lorsqu’une institution est sélectionnée, pour éviter de communiquer des données d’autres institutions.

Les documents déjà générés ou téléchargés ne changent pas automatiquement de présentation : générer un nouveau document applique la mise en forme actuelle.
