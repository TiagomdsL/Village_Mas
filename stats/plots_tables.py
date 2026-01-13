import json
import glob
import pandas as pd
import matplotlib.pyplot as plt
from collections import defaultdict

DATA_PATH = "data/*.json"

games = []
for file in glob.glob(DATA_PATH):
    with open(file, "r") as f:
        games.append(json.load(f))

game_results = []
role_stats = defaultdict(lambda: {"total": 0, "survived": 0})
rounds_per_game = []

for game in games:
    events = game["events"]

    # número de rondas
    max_round = max(e["round"] for e in events)
    rounds_per_game.append(max_round)

    # último estado de jogadores vivos
    alive_events = [e for e in events if e["type"] == "ALIVE_PLAYERS"]
    final_alive = alive_events[-1]["data"]

    alive_roles = [p["role"] for p in final_alive]

    # vitória
    if "WEREWOLF" in alive_roles:
        winner = "WEREWOLVES"
    else:
        winner = "VILLAGERS"

    game_results.append({
        "winner": winner,
        "rounds": max_round
    })

    # descobrir todos os jogadores e roles iniciais
    first_alive = alive_events[0]["data"]
    initial_players = {p["name"]: p["role"] for p in first_alive}
    final_players = {p["name"] for p in final_alive}

    for name, role in initial_players.items():
        role_stats[role]["total"] += 1
        if name in final_players:
            role_stats[role]["survived"] += 1



# Tabela de roles por taxa de sobrevivência

df_games = pd.DataFrame(game_results, columns=["winner"])
winrate = df_games["winner"].value_counts(normalize=True) * 100

df_roles = pd.DataFrame([
    {
        "role": role,
        "survival_rate (%)": 100 * stats["survived"] / stats["total"]
    }
    for role, stats in role_stats.items()
]).sort_values("survival_rate (%)", ascending=False)


plt.figure(figsize=(6, 0.5 * len(df_roles) + 1))
plt.axis("off")

table = plt.table(
    cellText=df_roles.round(2).values,
    colLabels=df_roles.columns,
    cellLoc="center",
    colLoc="center",
    loc="center"
)

table.scale(1, 1.6)
table.auto_set_font_size(False)
table.set_fontsize(10)

for (row, col), cell in table.get_celld().items():
    if row == 0:
        cell.set_text_props(weight="bold")
        cell.set_height(0.08)
        cell.set_facecolor("#D4D4D4")
    else:
        cell.set_height(0.07)
        if row % 2 == 0:
            cell.set_facecolor("#F7F7F7")

plt.title("Survival Rate per Role", pad=20)

plt.savefig("survival_by_role_table.png", dpi=300, bbox_inches="tight")
plt.close()

# Winrate plot
plt.figure(figsize=(5, 4))
bars = plt.bar(winrate.index, winrate.values)
plt.title("Winrate")
plt.ylabel("Percentage")
plt.grid(axis="y", linestyle="--", alpha=0.6)
plt.gca().set_axisbelow(True)

for bar in bars:
    height = bar.get_height()
    plt.text(
        bar.get_x() + bar.get_width() / 2,
        height,
        f"{height:.1f}%",
        ha="center",
        va="bottom"
    )

plt.tight_layout()
plt.savefig("winrate_plot.png", dpi=300)
plt.close()

# Survival per role plot
plt.figure(figsize=(7, 4))
bars = plt.bar(df_roles["role"], df_roles["survival_rate (%)"])
plt.title("Survival Rate per Role")
plt.ylabel("Percentage")
plt.xticks(rotation=45)
plt.grid(axis="y", linestyle="--", alpha=0.6)
plt.gca().set_axisbelow(True)

for bar in bars:
    height = bar.get_height()
    plt.text(
        bar.get_x() + bar.get_width() / 2,
        height,
        f"{height:.1f}%",
        ha="center",
        va="bottom",
        fontsize=9
    )

plt.tight_layout()
plt.savefig("survival_rate_plot.png", dpi=300)
plt.close()

# Average rounds per game outcome plot

df_games = pd.DataFrame(game_results)

avg_rounds_all = df_games["rounds"].mean()
avg_rounds_villagers = df_games[df_games["winner"] == "VILLAGERS"]["rounds"].mean()
avg_rounds_werewolves = df_games[df_games["winner"] == "WEREWOLVES"]["rounds"].mean()

labels = [
    "All Games",
    "Villagers Win",
    "Werewolves Win"
]

values = [
    avg_rounds_all,
    avg_rounds_villagers,
    avg_rounds_werewolves
]

plt.figure(figsize=(7, 4))
bars = plt.bar(labels, values)
plt.ylabel("Average Number of Rounds")
plt.title("Game Duration by Outcome")
plt.grid(axis="y", linestyle="--", alpha=0.6)
plt.gca().set_axisbelow(True)

for bar in bars:
    height = bar.get_height()
    plt.text(
        bar.get_x() + bar.get_width() / 2,
        height,
        f"{height:.2f}",
        ha="center",
        va="bottom"
    )

plt.tight_layout()
plt.savefig("average_rounds_summary.png", dpi=300)
plt.close()
