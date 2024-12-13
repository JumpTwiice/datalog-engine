import json
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path

BASE_PATH = Path(__file__).parent.parent.absolute()
RESULT_PATH = BASE_PATH / "result"
SOUFFLE_PATH = BASE_PATH / "souffle" / "result" 

# plt.rcParams["text.usetex"] = True

def load_json_data(filename):
    """Load JSON data from file"""
    with open(filename, "r") as file:
        return json.load(file)

def filter_and_sort_data(data):
    """Filter out entries with -1 values and sort by values"""
    filtered_data = map(lambda x: (int(x[0]), np.nan) if x[1] == -1 else (int(x[0]),x[1]), data.items())
    return dict(sorted(filtered_data, key=lambda x: x[0]))

def get_solver_data(filename):
    """Get filtered and sorted solver data from a JSON file"""
    return filter_and_sort_data(load_json_data(filename))

def get_all_solver_data(filename):
    """Get filtered and sorted data for all solvers in a JSON file"""
    data = load_json_data(filename)
    return {solver: filter_and_sort_data(data[solver]) for solver in data.keys()}

def get_souffle_average(problem_name, num):
    """Compute the average of 'num' Soufflé runs for a given problem"""
    results = [get_souffle_data(SOUFFLE_PATH / problem_name / f"{i}.txt") for i in range(1, num+1)]
    return sum(results) / len(results)

def get_souffle_data(filename):
    """Extract the total runtime from a single Soufflé run"""
    with open(filename, "r") as file:
        lines = file.readlines()[3:-1] 
    return sum(float(line.strip().partition("s")[0]) for line in lines) * 1000

# def souffle_to_dict(problems):
#     res = {}
#     for problem in problems:
#         res[problem] = get_souffle_avg(SOUFFLE_PATH / problem / f"/{i}.txt")
#     return res
    
def normalize(data, f):
    """Normalize data using function f"""
    for solver in data:
        data[solver] = {k: v / f(int(k)) for k, v in data[solver].items()}

def semi_vs_naive(solver, filename):
    """Compare semi-naive and naive solvers"""
    semi_data = get_all_solver_data(RESULT_PATH / "semi-naive" / filename)[solver]
    naive_data = get_all_solver_data(RESULT_PATH / "naive" / filename)[solver]
    
    data = {
        f"{solver} with Semi-Naive": semi_data,
        f"{solver} with Naive": naive_data
    }
    return data

def get_min(data):
    return min(map(lambda x: int(x), list(data[next(iter(data))].keys())))

def get_max(data):
    return max(map(lambda x: int(x), list(data[next(iter(data))].keys())))

def scc_trie_solver_semi_vs_naive():
    reach_data = semi_vs_naive("SCC Trie Solver", "default-reachable.json")
    clusters_data = semi_vs_naive("SCC Trie Solver", "default-clusters.json")
    reach_min = get_min(reach_data)
    reach_max = get_max(reach_data)
    clusters_min = get_min(clusters_data)
    clusters_max = get_max(clusters_data)
    plot(reach_data, "Sequence - Naive vs Semi-naive", "No. of Edge EDB Facts", np.arange(reach_min, reach_max + 1, 40))
    plot(clusters_data, "Clusters - Naive vs Semi-naive", "No. of Edge EDB Facts", np.arange(clusters_min, clusters_max + 1, 500))

def plot(data, title, x_label, x_ticks):
    colors = ["red", "blue", "green", "purple"]
    for solver, color in zip(data.keys(), colors):
        x = list(data[solver].keys())
        y = list(data[solver].values())
        plt.plot(x, y, "o", label=solver, color=color)
    plt.xlabel(x_label)
    plt.ylabel("Time [ms]")
    plt.title(title)
    plt.xticks(x_ticks)
    plt.legend()
    plt.grid()
    plt.show()

def main():
    scc_trie_solver_semi_vs_naive()
    naive_reachable_data = get_all_solver_data(RESULT_PATH / "naive" / "reachable.json")
    semi_reachable_data = get_all_solver_data(RESULT_PATH / "semi-naive" / "reachable.json")
    data = {
        "Trie Solver with Semi-Naive": semi_reachable_data["Trie Solver"],
        "Trie Solver with Naive": naive_reachable_data["Trie Solver"],
        "Simple Solver with Semi-Naive": semi_reachable_data["Simple Solver"],
        "Simple Solver with Naive": naive_reachable_data["Simple Solver"]
    }
    plot(data, "Sequence", "No. of edge EDB Facts", np.arange(30, 131, 10))
    exit(0)
    
    print("Soufflé cartesian: " + str(get_souffle_average("cartesian", 10)) + " ms")
    print("Soufflé reachable: " + str(get_souffle_average("reachable", 10)) + " ms")
    print("Soufflé reachable-flipped: " + str(get_souffle_average("reachable-flipped", 10)) + " ms")

    #semi_vs_naive("Simple Solver", "Trie Solver")
    
    scc_reachable_data = get_all_solver_data(RESULT_PATH / "semi-naive" / "scc-reachable.json")
    plot(scc_reachable_data, "SCC-Reachable - Semi-Naive Evaluation", "Depth", np.arange(1, 17, 1))

    
    

if __name__ == "__main__":
    main()
