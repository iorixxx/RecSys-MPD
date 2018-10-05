import argparse


CLI = argparse.ArgumentParser()

CLI.add_argument("output", help="Absolute path of the output txt file")
CLI.add_argument("--letors", help="Absolute paths of letor txt files to be merged", nargs="+", required=True)


def merge(output_path, files):
    with open(output_path, "w") as out:
        i = 0
        for file in files:
            i += 1
            with open(file, "r") as f:
                for line in f:
                    if i == 1 or not line.startswith("#"):
                        out.write(line)

    print("Files are merged into: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    merge(output_path=args.output, files=args.letors)
