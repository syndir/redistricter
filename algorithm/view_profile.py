import pstats
import sys

if __name__ == "__main__":
    if len(sys.argv) == 2:
        profile = sys.argv[1]
        s = pstats.Stats(profile)
        s.sort_stats('tottime')
        s.print_stats()
    elif len(sys.argv) == 3:
        profile = sys.argv[1]
        sort = sys.argv[2]
        s = pstats.Stats(profile)
        s.sort_stats(sort)
        s.print_stats()
    else:
        s = pstats.Stats("profile.out")
        s.sort_stats('tottime')
        s.print_stats()
