from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether

OUTPUT = r"C:\Users\IT SOLUTION SARL\Intelligentsia\application-monitoring-intelligentsia-main\output\pdf\Guide_utilisateur_Monitoring_Intelligentsia.pdf"
NAVY = colors.HexColor("#17263B")
GOLD = colors.HexColor("#C9A84C")
BLUE = colors.HexColor("#1976A5")
PALE = colors.HexColor("#F2F5F8")
MUTED = colors.HexColor("#58677A")

styles = getSampleStyleSheet()
styles.add(ParagraphStyle(name="CoverTitle", parent=styles["Title"], fontName="Helvetica-Bold", fontSize=27, leading=33, textColor=NAVY, alignment=TA_LEFT, spaceAfter=12))
styles.add(ParagraphStyle(name="CoverSub", parent=styles["Normal"], fontSize=13, leading=20, textColor=MUTED, spaceAfter=10))
styles.add(ParagraphStyle(name="H1x", parent=styles["Heading1"], fontName="Helvetica-Bold", fontSize=18, leading=23, textColor=NAVY, spaceBefore=3, spaceAfter=10, keepWithNext=True))
styles.add(ParagraphStyle(name="H2x", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=11.5, leading=15, textColor=BLUE, spaceBefore=8, spaceAfter=4, keepWithNext=True))
styles.add(ParagraphStyle(name="Bodyx", parent=styles["BodyText"], fontSize=9.2, leading=14, textColor=colors.HexColor("#263547"), spaceAfter=6))
styles.add(ParagraphStyle(name="Smallx", parent=styles["BodyText"], fontSize=7.8, leading=10.5, textColor=MUTED, spaceAfter=3))
styles.add(ParagraphStyle(name="Cellx", parent=styles["BodyText"], fontSize=7.6, leading=10, textColor=colors.HexColor("#263547")))
styles.add(ParagraphStyle(name="CellHeadx", parent=styles["BodyText"], fontName="Helvetica-Bold", fontSize=7.8, leading=10, textColor=colors.white))
styles.add(ParagraphStyle(name="Calloutx", parent=styles["BodyText"], fontSize=9, leading=13, textColor=NAVY, backColor=PALE, borderColor=GOLD, borderWidth=1, borderPadding=8, spaceBefore=5, spaceAfter=8))

def P(text, style="Bodyx"):
    return Paragraph(text, styles[style])

def table(rows, widths, header=True):
    converted = []
    for ri, row in enumerate(rows):
        converted.append([Paragraph(str(cell), styles["CellHeadx"] if header and ri == 0 else styles["Cellx"]) for cell in row])
    t = Table(converted, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [("VALIGN", (0,0), (-1,-1), "TOP"), ("GRID", (0,0), (-1,-1), .35, colors.HexColor("#D9E0E8")), ("LEFTPADDING", (0,0), (-1,-1), 6), ("RIGHTPADDING", (0,0), (-1,-1), 6), ("TOPPADDING", (0,0), (-1,-1), 5), ("BOTTOMPADDING", (0,0), (-1,-1), 5)]
    if header:
        commands.append(("BACKGROUND", (0,0), (-1,0), NAVY))
        if len(rows) > 1: commands.append(("ROWBACKGROUNDS", (0,1), (-1,-1), [colors.white, PALE]))
    t.setStyle(TableStyle(commands))
    return t

def footer(canvas, doc):
    canvas.saveState()
    w, h = A4
    canvas.setStrokeColor(colors.HexColor("#D9E0E8")); canvas.setLineWidth(.5)
    canvas.line(18*mm, 15*mm, w-18*mm, 15*mm)
    canvas.setFont("Helvetica", 7.5); canvas.setFillColor(MUTED)
    canvas.drawString(18*mm, 10*mm, "Monitoring Intelligentsia - Guide utilisateur")
    canvas.drawRightString(w-18*mm, 10*mm, f"Page {doc.page}")
    canvas.restoreState()

story=[]
# Cover
story += [Spacer(1, 26*mm), P("MONITORING<br/>INTELLIGENTSIA", "CoverTitle"), P("Guide détaillé des pages et des fonctions de l’application de surveillance monétique", "CoverSub"), Spacer(1, 8*mm)]
story += [table([["Périmètre", "Ce que couvre ce guide"], ["Navigation", "Tableau de bord, terminaux, fraude, alertes, extractions et administration."], ["Données", "Les pages affichent les données retournées par le backend et filtrées selon le compte et l’institution."], ["Version décrite", "État du projet au 30 septembre 2026, incluant la configuration du risque et la création d’alertes de test." ]], [35*mm, 125*mm])]
story += [Spacer(1, 12*mm), P("Ce document explique à quoi sert chaque page, comment s’en servir et ce qui doit être alimenté pour obtenir des résultats. Les libellés peuvent varier légèrement selon les permissions du compte connecté.", "Bodyx"), Spacer(1, 55*mm), P("Document de référence en français", "Smallx"), PageBreak()]

story += [P("1. Comprendre l’application", "H1x"), P("Monitoring Intelligentsia rassemble dans une même interface la disponibilité des GAB, le suivi des réseaux et des transactions, la détection de fraude, le traitement des alertes, les extractions et l’administration. Le menu latéral n’affiche que les fonctions accessibles au compte connecté.")]
story += [P("Repères de navigation", "H2x"), table([
 ["Zone", "Pages", "Usage principal"],
 ["Surveillance", "Tableau de bord, GABs / ATMs, TPE / POS, Transactions, Fraude, Alertes, Archives, Flotte", "Voir l’état du parc et traiter les anomalies."],
 ["Moteur d’extractions", "Extraction ATM, Extraction POS, Templates SQL, Configurations, Historique", "Préparer, exécuter et consulter des extractions."],
 ["Administration", "Utilisateurs, Institutions, Rôles, Planificateur, Journal d’audit, Paramètres, Règles", "Gérer les accès, les paramètres et les règles de détection."],
 ["Compte", "Notifications, Profil, Connexion et récupération du mot de passe", "Consulter les événements personnels et gérer sa session."],
], [30*mm, 58*mm, 72*mm])]
story += [P("Filtre d’institution", "H2x"), P("La barre supérieure permet de choisir une institution lorsque le compte dispose d’un périmètre global. Un compte rattaché à une institution voit les données autorisées pour celle-ci. Les permissions cachent aussi certaines pages ou actions.")]
story += [P("Comprendre les données vides", "H2x"), P("Une page vide peut signifier qu’aucune donnée n’a été collectée sur la période, que le filtre ne correspond à aucun enregistrement, que la session a expiré ou que le service source n’est pas démarré. Les pages affichent un état vide ou une erreur selon le cas; le tableau de bord dépend notamment des snapshots de terminaux, des taux de transactions et des snapshots réseau.", "Calloutx"), PageBreak()]

story += [P("2. Tableau de bord", "H1x"), P("Le tableau de bord donne une vue rapide de la situation opérationnelle. Il actualise automatiquement ses données à intervalles réguliers et propose une action d’actualisation manuelle.")]
story += [P("Indicateurs principaux", "H2x"), table([
 ["Bloc", "Contenu"], ["GAB", "Nombre total, en ligne, hors ligne, avertissements et inactifs, selon les derniers snapshots disponibles."],
 ["TPE", "Nombre de terminaux POS suivis et terminaux sans télécollecte."], ["Alertes fraude", "Alertes récentes et volume de fraudes selon les données de fraude."],
 ["Taux de succès", "Rapport entre les transactions réussies et le total réussi + échoué du dernier relevé de taux."],
 ["Réseaux et hôtes", "État opérationnel le plus récent des réseaux et serveurs observés."],
 ["Courbes", "Historique récent de disponibilité des GAB et de fraudes détectées."],
], [38*mm, 122*mm])]
story += [P("Lire l’état des GAB", "H2x"), P("Le graphique en anneau répartit les terminaux entre en ligne, avertissement et hors ligne. Les compteurs viennent des snapshots : si aucun snapshot n’existe, le dashboard ne peut pas déduire l’état actuel à partir de la seule fiche du terminal.")]
story += [P("Dépendances", "H2x"), P("Les blocs réseau et taux de succès nécessitent des lignes dans les tables de snapshots réseau et taux transactionnels. Les données de démonstration sont explicitement marquées TEST et ne représentent pas la production. En environnement réel, les connecteurs d’alimentation doivent écrire des snapshots à jour.", "Calloutx"), PageBreak()]

story += [P("3. GAB, TPE et transactions", "H1x"), P("GABs / ATMs", "H2x"), P("La liste des GAB permet de rechercher un terminal et de parcourir les terminaux accessibles. La sélection d’un terminal ouvre sa fiche détaillée avec ses informations et l’historique de snapshots disponibles. Les états et seuils de billets sont basés sur les relevés remontés par le système; un terminal sans relevé n’a pas d’état récent fiable.")]
story += [P("TPE / POS et techniciens", "H2x"), P("La page TPE/POS recense les terminaux de paiement avec leurs données d’identification et d’implantation. La page Techniciens sert à consulter le personnel technique associé. Les filtres et recherches aident à retrouver un terminal, un commerçant ou un technicien.")]
story += [P("Transactions", "H2x"), P("La page Transactions permet de consulter les transactions disponibles et d’utiliser la recherche et les filtres proposés. Les résultats dépendent des flux connectés au backend; la page ne génère pas elle-même les opérations transactionnelles.")]
story += [P("Flotte", "H2x"), P("La page Flotte suit les comptes et encours surveillés, les variations entre snapshots et les cartes dépassant leurs limites lorsqu’elles sont alimentées. Elle sert à repérer des écarts et dépassements, pas à effectuer des opérations bancaires.")]
story += [PageBreak()]

story += [P("4. Surveillance de la fraude", "H1x"), P("La page Fraude réunit les indicateurs agrégés et les enregistrements détaillés fournis par les données de fraude. Le sélecteur de période couvre les dernières 24 heures, 7 jours ou 30 jours.")]
story += [P("Taux et Détails", "H2x"), table([
 ["Vue", "Ce qu’elle présente", "Interaction"],
 ["Taux", "Agrégats par BIN : suspects, fraudes, institution et date du snapshot.", "La ligne ouvre un résumé de l’agrégat sélectionné."],
 ["Détails", "Enregistrements avec règle, description et informations complémentaires présentes dans le détail.", "La ligne ouvre la fiche de fraude avec les champs disponibles."],
], [23*mm, 78*mm, 59*mm])]
story += [P("Graphique horaire", "H2x"), P("Pour la période de 24 heures, le graphique forme 24 tranches horaires consécutives, y compris les heures sans événement, qui sont affichées à zéro. Les barres sont fines pour mieux distinguer les heures. Les périodes plus longues conservent une tranche par heure et peuvent se parcourir horizontalement.")]
story += [P("Indicateurs et recherche", "H2x"), P("Les cartes résument les transactions suspectes, les fraudes confirmées et les BIN touchés. Les panneaux latéraux classent les BIN et les règles les plus présents. La recherche de la table accepte les critères proposés par l’interface. Les agrégats ne contiennent pas nécessairement les données d’une transaction individuelle; la fenêtre affiche uniquement les détails réellement fournis par l’API.", "Calloutx"), PageBreak()]

story += [P("5. Alertes, notifications et archives", "H1x"), P("Une alerte de surveillance est créée à partir d’une règle et reçoit son type et son niveau de risque depuis la règle enregistrée. La page Alertes permet de rechercher et filtrer par statut, catégorie et niveau.")]
story += [P("Cycle de traitement", "H2x"), table([
 ["Étape", "Sens"], ["Ouverte", "Nouvelle alerte à examiner."], ["Prise en compte", "Un opérateur confirme qu’il la traite."], ["En cours", "L’analyse ou l’intervention est commencée."], ["Résolue", "Le problème est traité."], ["Archivée", "Le suivi est terminé; l’historique est conservé dans Archives."],
], [38*mm, 122*mm])]
story += [P("Notifications et son", "H2x"), P("La cloche affiche le nombre de notifications non lues. Les notifications sont transmises en temps réel à la session active. Pour les niveaux Moyen et Critique, le serveur marque une notification sonore et l’interface joue le fichier d’alarme; les niveaux Typique et Moins critique restent visibles dans la cloche sans alarme sonore. Le navigateur doit autoriser la lecture audio et l’utilisateur doit être connecté pour recevoir l’événement en direct.")]
story += [P("Archives", "H2x"), P("La page Archives retrouve les alertes archivées, permet d’en ouvrir le détail et de consulter leur historique de traitement. Une alerte archivée est consultable mais ne reprend pas le cycle courant.")]
story += [P("Créer une alerte de test depuis Règles", "H2x"), P("Dans la page Règles, choisir une règle active dont les notifications sont autorisées, puis choisir un GAB de test dont le nom contient TEST et cliquer sur Créer et notifier. L’alerte est enregistrée avec le préfixe [TEST], liée au GAB choisi et apparaît dans Alertes. Si le niveau sauvegardé est Moyen ou Critique, une notification urgente et le son sont envoyés; les autres niveaux n’émettent pas de son. Le compteur de la cloche se met à jour pour les destinataires actifs autorisés à consulter les alertes.", "Calloutx"), PageBreak()]

story += [P("6. Extraction des données", "H1x"), P("Les pages d’extraction servent à lancer des extractions sur les périmètres ATM et POS. Le moteur utilise les templates SQL et les configurations qui lui sont affectés; l’historique permet de suivre les lots terminés ou en erreur.")]
story += [P("Pages du moteur", "H2x"), table([
 ["Page", "Fonction"], ["Extraction ATM", "Préparer et soumettre un lot d’extraction lié aux terminaux ATM."], ["Extraction POS", "Préparer et soumettre un lot d’extraction lié au périmètre POS."], ["Templates SQL", "Consulter et gérer les modèles de requêtes autorisés par l’application."], ["Configurations", "Régler les paramètres de connexion et de sélection utilisés par le moteur."], ["Historique", "Consulter les lots, leur type, dates et résultats de traitement."],
], [40*mm, 120*mm])]
story += [P("Bonnes pratiques", "H2x"), P("Avant de lancer une extraction, vérifier le type, le périmètre, les dates, le template choisi et les droits du compte. Le moteur ne doit utiliser que les sources configurées pour l’environnement actif. Les fichiers produits et l’historique dépendent du backend et de la disponibilité de ces sources.")]
story += [PageBreak()]

story += [P("7. Administration", "H1x"), table([
 ["Page", "Responsabilité"], ["Utilisateurs", "Gérer les comptes, leurs rôles, statuts et rattachements institutionnels selon les droits."], ["Institutions", "Consulter et administrer les institutions et leurs paramètres d’identification."], ["Rôles", "Associer des permissions aux profils d’accès."], ["Planificateur", "Consulter et gérer les tâches planifiées de collecte et de maintenance."], ["Journal d’audit", "Rechercher les actions enregistrées, leur acteur, date et entité."], ["Paramètres", "Configurer les paramètres d’application exposés à l’administration."], ["Règles", "Classer les règles par Fraude, GAB ou Réseau, régler leur niveau, pourcentage, notifications et état."],
], [37*mm, 123*mm])]
story += [P("Configurer une règle", "H2x"), P("Le Type d’alerte attribue la règle à Fraude, GAB ou Réseau. Le Niveau de risque propose Typique, Moins critique, Moyen et Critique. Les changements de niveau et de pourcentage restent en brouillon jusqu’au clic sur Valider. Le Pourcentage de risque est une valeur configurable de 0 à 100 %; la Part des alertes demeure une statistique calculée à partir des alertes déjà enregistrées. Ces deux valeurs ont des sens différents. Le pourcentage est actuellement stocké comme paramètre de la règle; il n’est pas encore utilisé comme seuil de déclenchement par un moteur de règles transactionnelles.")]
story += [P("Effet du niveau", "H2x"), P("Les alertes Moyen et Critique déclenchent le son et la notification urgente. Typique et Moins critique déclenchent la notification visuelle dans la cloche seulement. L’état Active/Désactivée détermine si la règle peut servir à créer ou générer des alertes.")]
story += [P("Données conservées", "H2x"), P("Le pourcentage assigné est stocké séparément des statistiques dans le schéma. La migration V20 ajoutée pour cette fonction ajoute une colonne avec une valeur initiale de 0 et une contrainte de plage; elle ne supprime pas les règles ni les alertes existantes.", "Calloutx"), PageBreak()]

story += [P("8. Profil, notifications et accès", "H1x"), P("Notifications", "H2x"), P("La page Notifications présente l’historique des notifications reçues et permet de les marquer comme lues. Cliquer sur une notification d’alerte ouvre l’alerte liée lorsque son identifiant est disponible. La cloche de l’en-tête donne accès au panneau rapide et au compteur non lu.")]
story += [P("Profil", "H2x"), P("La page Profil regroupe les informations du compte connecté et les options personnelles exposées par l’application.")]
story += [P("Connexion et récupération", "H2x"), P("La page Connexion ouvre la session. Mot de passe oublié et Réinitialiser le mot de passe suivent le processus de récupération prévu par le backend. Une session expirée peut interrompre la réception des notifications temps réel; se reconnecter rétablit le canal de notifications.")]
story += [P("Permissions", "H2x"), P("L’accès dépend des permissions définies par rôle et, pour les données institutionnelles, du périmètre du compte. Si un lien n’apparaît pas ou une action est refusée, vérifier le profil et les permissions avant de conclure que les données ont disparu.")]
story += [PageBreak()]

story += [P("9. Parcours de test recommandé", "H1x"), table([
 ["Ordre", "Action", "Résultat attendu"], ["1", "Se connecter avec un compte administrateur de test.", "Le tableau de bord et les menus autorisés s’ouvrent."], ["2", "Vérifier que des GAB de test sont disponibles.", "Au moins un terminal ou institution est identifié par TEST."], ["3", "Dans Règles, modifier un niveau et un pourcentage puis cliquer Valider.", "La valeur reste affichée après actualisation de la page."], ["4", "Créer et notifier une alerte de test avec ce GAB et cette règle.", "L’alerte porte [TEST], est liée au terminal et apparaît dans Alertes."], ["5", "Regarder la cloche et écouter le résultat.", "Le compteur augmente; Moyen/Critique joue le son, les autres niveaux restent silencieux."], ["6", "Traiter l’alerte puis l’archiver.", "Le statut évolue et l’alerte est visible dans Archives."],
], [14*mm, 83*mm, 63*mm])]
story += [P("Si le test n’aboutit pas", "H2x"), P("Vérifier que V20 s’est appliquée au redémarrage du backend, que la règle est active et autorise les notifications, que le GAB est bien identifié comme TEST, que l’utilisateur possède MONITORING_ALERT_VIEW, et que la session est connectée. Pour le son, vérifier le volume et l’autorisation de lecture audio du navigateur.")]
story += [P("Périmètre des données de test", "H2x"), P("Les alertes de test sont marquées [TEST] pour rester reconnaissables dans les listes. Elles sont de vraies lignes de test dans l’environnement courant; ne pas sélectionner un terminal de production pour un test. Les pages de monitoring ne remplacent pas le flux réel SVFE ou les sources bancaires. Ce guide n’active ni ne modifie à lui seul une connexion Oracle/SVFE.", "Calloutx")]
story += [Spacer(1, 5*mm), P("Fin du guide", "Smallx")]

doc = SimpleDocTemplate(OUTPUT, pagesize=A4, rightMargin=20*mm, leftMargin=20*mm, topMargin=18*mm, bottomMargin=21*mm, title="Guide utilisateur Monitoring Intelligentsia", author="Monitoring Intelligentsia")
doc.build(story, onFirstPage=footer, onLaterPages=footer)
print(OUTPUT)
