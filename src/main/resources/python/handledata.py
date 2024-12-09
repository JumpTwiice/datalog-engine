import json
import matplotlib.pyplot as plt
from pathlib import Path

path = Path(__file__).parent.parent.absolute()


def get_data(filename):
    with open(filename, "r") as file:
        data = json.load(file)
    for solver in data.keys():
        data[solver] = dict(sorted(data[solver].items(), key=lambda x: int(x[0])))
    return data


def main():
    semi_data = get_data(f"{path}\\result\\semi-naive\\hard-problem.json")
    colors = ["red", "blue", "green", "purple"]

    for solver, color in zip(semi_data.keys(), colors):
        x = list(semi_data[solver].keys())
        y = list(semi_data[solver].values())

        plt.plot(x, y, "o", label=solver, color=color)

    plt.xlabel("Edge relation size")
    plt.ylabel("Time [ms]")
    plt.title("Hard Problem")
    plt.legend()
    plt.show()

if __name__ == "__main__":
    main()
