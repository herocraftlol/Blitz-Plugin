# Blitz - Plugin Minecraft (1.21) pour HeroCraft

## Compilation

Ce projet est un projet Maven. Comme mon environnement de travail n'a pas accès à Internet,
je n'ai pas pu exécuter `mvn package` pour vérifier la compilation finale — il faudra le faire
de votre côté (le code a été relu attentivement mais un `mvn clean package` reste nécessaire
pour confirmer qu'il n'y a pas de coquille).

```bash
cd blitz-plugin
mvn clean package
```

Le jar généré se trouve dans `target/Blitz.jar`. Placez-le dans le dossier `plugins/` de votre
serveur (Paper/Spigot **1.21** requis).

## Mise en place

1. Démarrez le serveur une première fois pour générer `plugins/Blitz/config.yml`.
2. Définissez le spawn du lobby : `/blitz setlobby` (en te tenant à l'endroit voulu).
3. Créez une arène : `/blitz create <nom>`.
4. Récupérez la baguette de sélection : `/blitz wand` (clic gauche = pos1, clic droit = pos2).
5. Sélectionnez la zone qui doit se réinitialiser à chaque partie (bâtiments, sol, etc.) puis :
   `/blitz setregion <nom>`.
6. Sélectionnez le trou (la zone où l'équipe adverse doit sauter) de chaque équipe puis :
   `/blitz setgoal red <nom>` et `/blitz setgoal blue <nom>`.
7. Définissez les spawns : `/blitz setspawn red <nom>` et `/blitz setspawn blue <nom>`.
8. Ajoutez des coffres de butin : regardez un coffre déjà posé dans la map puis
   `/blitz addchest <nom>` (autant de fois que nécessaire).
9. (Optionnel) `/blitz setlimit <nom> <score>` (5 par défaut) et
   `/blitz setteamsize <nom> <1-8>` pour la taille maximale d'équipe (1v1 à 8v8).
10. Les joueurs rejoignent avec `/blitz join <nom>`, `/blitz gui` (menu graphique avec option
    aléatoire) ou en cliquant dans le menu.

## Commandes joueurs
- `/blitz join <arène>`
- `/blitz leave`
- `/blitz gui`
- `/blitz stats [joueur]`

## Commandes admin (`blitz.admin`)
- `/blitz wand`, `/blitz create`, `/blitz delete`, `/blitz list`
- `/blitz setlobby`, `/blitz setspawn <red|blue> <arène>`
- `/blitz setregion <arène>`, `/blitz setgoal <red|blue> <arène>`
- `/blitz setlimit <arène> <score>`, `/blitz setteamsize <arène> <1-8>`
- `/blitz addchest <arène>`, `/blitz forcestart <arène>`
- `/blitz hologram <wins|played|kd|kills>` (crée le classement holographique à vos pieds)
- `/blitz delhologram` (supprime l'hologramme le plus proche)
- `/blitz reload`

## Fonctionnement du jeu
- Blitz = 2 équipes (Rouge/Bleu), on marque en sautant dans le trou adverse.
- Premier à atteindre le score configuré (5 par défaut) gagne.
- La zone définie par `/blitz setregion` est protégée : impossible de casser les blocs
  d'origine, mais on peut poser des blocs et casser ceux qu'on a soi-même posés.
- Armure en cuir incassable (couleur d'équipe, non lâchable à la mort) + épée en bois incassable.
- Coffres de butin (épée en pierre, laine/terracotta d'équipe, arc, flèches, pommes en or,
  potions) qui se réapprovisionnent toutes les 10 minutes (configurable dans `config.yml`).
- Sidebar en temps réel : nom du serveur, nom du jeu, map, temps, score des 2 équipes, K/D.
- 4 hologrammes de classement invocables (victoires, parties jouées, K/D, kills - top 10).
