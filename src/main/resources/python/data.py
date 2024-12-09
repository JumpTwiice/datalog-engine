import json

res_path = "~/Desktop/datalog-engine/src/main/resources/result/"

def main():
    with open(res_path + "semi-naive/hard-problem.json", "r") as file:
        data = json.load(file)




if __name__== "__main__":
    main()