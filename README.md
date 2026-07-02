# 🏆 Blitz - Plugin Minecraft (1.21) pour HeroCraft

[![Version](https://img.shields.io/badge/Version-2.0.3-blue)](https://github.com/herocraftlol/Blitz-Plugin/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21-green)](https://www.minecraft.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21-orange)](https://papermc.io/)

## 📝 Description

Plugin de jeu **Blitz** pour serveur Minecraft Paper 1.21. Un jeu compétitif en 2 équipes (Rouge/Bleu) où le but est de **sauter dans le trou adverse** pour marquer des points !

### ✨ Nouvelles fonctionnalités en v2.0.0

- 🎮 **Lobby d'attente** - Système de lobby avec retour automatique à la position initiale
- 🎲 **Join aléatoire** - Commande `/blitz joinrandom` pour rejoindre une arène aléatoire
- 🖥️ **GUI amélioré** - Interface graphique redesignée pour la sélection des arènes
- 📊 **Leaderboard complet** - Classements holographiques améliorés (wins, played, kd, kills)
- 🔄 **Commande `/arenas`** - Alias rapide pour accéder au GUI des arènes
- 🎒 **Objets de lobby** - Items personnalisés pour naviguer et quitter le jeu
- 📍 **Retour position** - Les joueurs retrouvent leur position exacte après avoir quitté une partie

### ✨ Fonctionnalités principales

- 🎮 **Système d'arènes complet** - Créez et gérez vos propres maps de Blitz
- ⚔️ **Combat équitable** - Épée en bois incassable + armure en cuir colorée (indestructible)
- 📦 **Coffres de butin** - Réapprovisionnement automatique avec équipement de combat
- 🏅 **Classements holographiques** - Top 10 en temps réel (Victoires, Parties, K/D, Kills)
- 📋 **Scoreboard dynamique** - Statistiques des équipes en temps réel
- 🛡️ **Protection des blocs** - Les structures sont protégées, seuls vos blocs sont cassables
- 🎯 **Système de goals** - Zone de victoire unique pour chaque équipe
- 🔄 **Réinitialisation d'arène** - La zone définie se réinitialise automatiquement entre les parties

## 📥 Téléchargement

**➡️ [Télécharger la dernière version](https://github.com/herocraftlol/Blitz-Plugin/releases/latest)**

Le fichier `Blitz.jar` doit être placé dans le dossier `plugins/` de votre serveur Paper/Spigot 1.21.

## 🚀 Installation rapide

1. Téléchargez `Blitz.jar` depuis la [page des releases](https://github.com/herocraftlol/Blitz-Plugin/releases)
2. Placez le fichier dans le dossier `plugins/` de votre serveur
3. Redémarrez votre serveur
4. Configurez votre première arène :

```bash
/blitz setlobby          # Définissez le spawn principal (lobby d'attente)
/blitz create <nom>      # Créez une nouvelle arène
/blitz wand              # Obtenez la baguette de sélection
# Faites clic gauche (pos1) et clic droit (pos2) pour sélectionner une zone
/blitz setregion <nom>   # Définissez la zone qui se réinitialise
/blitz setgoal red <nom> # Sélectionnez le goal équipe rouge
/blitz setgoal blue <nom># Sélectionnez le goal équipe bleue
/blitz setspawn red <nom>   # Spawn équipe rouge
/blitz setspawn blue <nom>  # Spawn équipe bleue
# Placez des coffres avec du loot puis :
/blitz addchest <nom>    # Ajoutez un coffre de butin
```

## 🎮 Comment jouer

Les joueurs rejoignent avec `/blitz join <arene>` pour une arène spécifique ou `/blitz joinrandom` pour rejoindre aléatoirement. Le lobby d'attente permet aux joueurs de se préparer avant la partie. Le premier équipe à atteindre le score cible (5 par défaut) gagne !

## ⚙️ Commandes

### Commandes joueurs
| Commande | Description |
|----------|-------------|
| `/blitz join <arene>` | Rejoindre une partie spécifique |
| `/blitz joinrandom` | Rejoindre une arène aléatoire |
| `/blitz leave` | Quitter la partie en cours (retour au lobby) |
| `/blitz gui` ou `/arenas` | Ouvrir le menu de sélection des arènes |
| `/blitz stats [joueur]` | Voir vos statistiques ou celles d'un joueur |

### Commandes Admin (permission `blitz.admin`)
| Commande | Description |
|----------|-------------|
| `/blitz wand` | Obtenir la baguette de sélection |
| `/blitz create <nom>` | Créer une nouvelle arène |
| `/blitz delete <nom>` | Supprimer une arène |
| `/blitz list` | Lister toutes les arènes |
| `/blitz setlobby` | Définir le spawn lobby (position d'attente) |
| `/blitz setspawn red/blue <arene>` | Définir les spawns d'équipe |
| `/blitz setregion <arene>` | Définir la zone de réinitialisation |
| `/blitz delregion <arene>` | Supprimer la zone de reset |
| `/blitz setgoal red/blue <arene>` | Définir le goal de l'équipe |
| `/blitz addchest <arene>` | Ajouter un coffre de butin |
| `/blitz setlimit <arene> <score>` | Score pour gagner (défaut: 5) |
| `/blitz setteamsize <arene> <1-8>` | Taille des équipes (1v1 à 8v8) |
| `/blitz forcestart <arene>` | Forcer le démarrage |
| `/blitz hologram wins/played/kd/kills` | Créer un classement holographique |
| `/blitz delhologram` | Supprimer l'hologramme le plus proche |
| `/blitz reload` | Recharger la configuration |

## 🔧 Configuration

Le fichier `plugins/Blitz/config.yml` est généré automatiquement au premier démarrage. Vous pouvez y modifier :
- Le délai de réapprovisionnement des coffres de butin
- Le score maximum pour gagner
- Les messages du plugin

## 🛠️ Compilation depuis les sources

Si vous souhaitez compiler le plugin vous-même :

```bash
git clone https://github.com/herocraftlol/Blitz-Plugin.git
cd Blitz-Plugin
mvn clean package
```

Le fichier JAR sera généré dans `target/Blitz.jar`.

**Prérequis :**
- Java 21
- Maven 3.6+

## 📄 Licence

Ce plugin a été développé par l'équipe **HeroCraft**.

## 📋 Changelog

### v2.0.3
- Mise a jour et ameliorations
- Corrections de bugs

### v2.0.2
- Ameliorations et nouvelles fonctionnalités
- Corrections de bugs
- Optimisations du code

### v2.0.1
- Correction du bug PotionType.SPEED -> SWIFTNESS (compatibilité Paper 1.21)
- Ajout de la commande `/blitz delregion` pour supprimer la zone de reset
- Ajout du listener `ChestListener` pour la gestion des coffre
- Améliorations générales et corrections de bugs

### v2.0.0
- Lobby d'attente avec retour automatique à la position initiale
- Commande `/blitz joinrandom` pour rejoindre une arène aléatoire
- GUI amélioré pour la sélection des arènes
- Leaderboard holographique complet (wins, played, kd, kills)
- Commande `/arenas` alias pour le GUI
- Objets de lobby personnalisés
- Système de retour position
