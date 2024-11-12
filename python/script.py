import random
import sys

random.seed(0)


def f(x):
    result = [random.random()]
    result.append(1 - result[0])
    if x < 50:
        result.reverse()
    return result


def main():
    args = sys.argv[1:]
    if len(args) != 1:
        print("Usage: python script.py <number>")
        sys.exit(1)
    try:
        x = float(args[0])
    except ValueError:
        print("Error: Invalid number")
        sys.exit(1)
    result = f(x)
    print("Result:", *result)


if __name__ == "__main__":
    main()
