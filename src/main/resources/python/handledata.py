import json
import matplotlib.pyplot as plt
from pathlib import Path

path = Path(__file__).parent.parent.absolute()

def get_solver_data(filename):
    with open(filename, "r") as file:
        data = json.load(file)
    filtered_data = filter(lambda x: x[1] != -1, data.items())
    sorted_data = sorted(filtered_data, key=lambda x: int(x[0]))
    data = dict(sorted_data)
    return data

def get_data(filename):
    with open(filename, "r") as file:
        data = json.load(file)
    for solver in data.keys():
        filtered_data = filter(lambda x: x[1] != -1, data[solver].items())
        mapped_data = map(lambda x: (int(x[0]), x[1]), filtered_data)
        sorted_data = sorted(mapped_data, key=lambda x: int(x[0]))
        data[solver] = dict(sorted_data)
    return data

def normalize(data, func):
    for solver in data.keys():
        mapping = map(lambda x: (x[0], x[1]/func(int(x[0]))), data[solver].items())
        data[solver] = dict(mapping)

def plot(data):
    # normalize(data, lambda x: x**3)
    colors = ["red", "blue", "green", "purple"]
    for solver, color in zip(data.keys(), colors):
        x = list(data[solver].keys())
        y = list(data[solver].values())
        plt.plot(x, y, "o", label=solver, color=color)
    plt.xlabel("Edge relation size")
    plt.ylabel("Time [ms]")
    plt.title("Hard Problem")
    plt.legend()
    plt.show()

def main():
    semi_data = get_data(f"{path}\\result\\semi-naive\\hard-problem.json")
    naive_data = get_data(f"{path}\\result\\naive\\hard-problem.json")
    trie_solver_data = get_data(f"{path}\\result\\semi-naive\\trie-solver-reachable.json")
    scc_reachable_data = get_data(f"{path}\\result\\semi-naive\\scc-reachable.json")
    plot(scc_reachable_data)

if __name__ == "__main__":
    main()
