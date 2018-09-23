import argparse


CLI = argparse.ArgumentParser()

CLI.add_argument("output", help="Absolute path of the output file")
CLI.add_argument("-m", "--merge", nargs="+", help="Absolute path of the output file", required=True)


def merge(output_path, files):
    with open(output_path, "w") as out:
        for file in files:
            with open(file, "r") as i:
                for line in i:
                    out.write(line)

    print("Files are merged into: %s" % output_path)


if __name__ == '__main__':
    args = CLI.parse_args()

    merge(output_path=args.output, files=args.merge)

