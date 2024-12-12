import json
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path

BASE_PATH = Path(__file__).parent.parent.absolute()
RESULT_PATH = BASE_PATH / "result"
SOUFFLE_PATH = BASE_PATH / "souffle" / "result" 

def load_json_data(filename):
    """Load JSON data from file"""
    with open(filename, "r") as file:
        return json.load(file)

def filter_and_sort_data(data):
    """Filter out entries with -1 values and sort by values"""
    filtered_data = map(lambda x: np.nan if x[1] == -1 else (int(x[0]),x[1]), data.items())
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
    return sum(results) / len(results) * 1000

def get_souffle_data(filename):
    """Extract the total runtime from a single Soufflé run"""
    with open(filename, "r") as file:
        lines = file.readlines()[3:-1] 
    return sum(float(line.strip().partition("s")[0]) for line in lines)

# def souffle_to_dict(problems):
#     res = {}
#     for problem in problems:
#         res[problem] = get_souffle_avg(SOUFFLE_PATH / problem / f"/{i}.txt")
#     return res
    
def normalize(data, f):
    """Normalize data using function f"""
    for solver in data:
        data[solver] = {k: v / f(int(k)) for k, v in data[solver].items()}

def semi_vs_naive(semi_solver, naive_solver):
    """Compare semi-naive and naive solvers"""
    semi_data = get_all_solver_data(RESULT_PATH / "semi-naive" / "hard-problem.json")[semi_solver]
    naive_data = get_all_solver_data(RESULT_PATH / "naive" / "hard-problem.json")[naive_solver]
    
    data = {
        f"{semi_solver} with Semi-Naive": semi_data,
        f"{naive_solver} with Naive": naive_data
    }
    plot(data, "Reachable - Naive vs Semi-Naive Evaluation", "No. of Edge Facts", np.arange(30, 131, 10))

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
    print("Soufflé reachable: " + str(get_souffle_average("reachable", 10)) + " ms")
    print("Soufflé reachable-flipped: " + str(get_souffle_average("reachable-flipped", 10)) + " ms")
    print("Soufflé cartesian: " + str(get_souffle_average("cartesian", 10)) + " ms")

    semi_vs_naive("Simple Solver", "Trie Solver")
    
    scc_reachable_data = get_all_solver_data(RESULT_PATH / "semi-naive" / "scc-reachable.json")
    plot(scc_reachable_data, "SCC-Reachable with Semi-Naive", "Depth", np.arange(1, 17, 1))

    naive_reachable_data = get_all_solver_data(RESULT_PATH / "naive" / "hard-problem.json")
    plot(naive_reachable_data, "Reachable - Naive Evaluation", "No. of Edge Facts", np.arange(30, 131, 10))

if __name__ == "__main__":
    main()
