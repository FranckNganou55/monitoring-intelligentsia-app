# Statut des GABs depuis les interfaces

La migration V25 historise déjà INTERFACES et INTERFACE_STATUS dans `atm_interface_snapshots` et les états de surveillance. V29 paramètre la cadence des alertes. Elles restent inchangées, car elles ont déjà été appliquées. V32 ajoute la vue `atm_display_status` pour harmoniser le statut affiché, ses filtres et les compteurs du tableau de bord.

| INT_STAT / IST_CODE | État source | Catégorie affichée |
|---|---|---|
| 0 | DOWN | Hors ligne |
| 1 | UP | Avertissement |
| 3 | SIGNOFF | Avertissement |
| 4 | SIGNON | Opérationnel |
| autre / absent | inconnu | Inconnu |

Le dernier relevé de l’interface correspondante est prioritaire sur le statut historique du terminal et sur `allOk`. Les seuils de cassettes ne changent pas cette correspondance. Les règles d’alertes après plusieurs relevés DOWN/SIGNOFF restent indépendantes de la catégorie de présentation.

La collecte Oracle existante utilise `INTERFACES.INT_STAT` et la jointure `INTERFACE_STATUS.IST_CODE` avec `IST_LABE`. Le fichier de référence contient bien **IST_LABE**, et non IST_LABEL. Les colonnes de ces tables ne sont pas modifiées.

Le rapprochement existant utilise le numéro du terminal dans `INT_IDEN` ou dans `INT_LABE`, avec suppression du préfixe `ATM_`. Pour les GABs de test BALANCE, le numéro original est lu dans leur jeu de test. Aucun rapprochement arbitraire n’est fait à partir du numéro de cassette, de la devise ou de l’ordre des lignes.

Les deux interfaces Excel fournies concernent **10008888** et **10008889**, toutes deux DOWN (0). Elles ne correspondent pas aux neuf GABs BALANCE **108401001 à 108401009**. Les neuf GABs de ce jeu restent donc Inconnu tant qu’aucun relevé d’interface correspondant n’est disponible. Les fichiers de cassettes et d’interfaces restent des sources distinctes.

Le tableau de bord inclut maintenant tous les GABs du périmètre, y compris ceux sans relevé, avec un compteur et un secteur gris Inconnu. La catégorie, la liste filtrée et le détail consultent la même vue.

Validation : tests PostgreSQL sur la vue et les endpoints, création de données transactionnelles temporaires avec rollback. Le test place un ancien statut terminal 4 en face d’une interface 0,1,3,4 puis 9, et vérifie la priorité de l’interface et les filtres. Tests frontend sur les codes et libellés. Aucun statut n’a été inventé ni injecté dans les neuf GABs BALANCE.

## Statuts synthétiques de démonstration

Après cette analyse, l’utilisateur a explicitement autorisé des statuts de test pour les 9 GABs BALANCE. Ils ont donc reçu des interfaces locales `TEST_BALANCE_...` : 3 SIGNON, 2 UP, 2 SIGNOFF et 2 DOWN. Ces statuts sont inventés uniquement pour tester la présentation, et ne sont pas des statuts lus dans les fichiers Excel ni dans Oracle. Les relevés des cassettes restent inchangés.
