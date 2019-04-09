import argparse

from os.path import join


CLI = argparse.ArgumentParser()

CLI.add_argument("output", help="Absolute path of the output folder")
CLI.add_argument("features", help="Number of feature options", type=int)


def generate(output_path, features):
    for i in range(features):
        val = str(i + 1)
        with open(join(output_path, "%s.txt" % val), "w") as out:
            out.write(val)

    print("Feature options are created in: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    generate(output_path=args.output, features=args.features)
