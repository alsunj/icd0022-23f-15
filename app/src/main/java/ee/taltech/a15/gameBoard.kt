package ee.taltech.a15

class gameBoard {
    private var numbers = (1..16).toList();

    fun createPuzzle(): List<Int> {
        var shuffledNumbers = numbers.shuffled()
        var inversionCount = 0
        var blankbuttonNumber = 0

        for (i in 0 until 16) {
            for (n in i until shuffledNumbers.size) {
                if (shuffledNumbers[i] != 16 && shuffledNumbers[i] > shuffledNumbers[n]) inversionCount++
                else if (shuffledNumbers[i] == 16) blankbuttonNumber = when (i) {
                    in 1..4 -> 1
                    in 5..8 -> 2
                    in 9..12 -> 3
                    else -> 4
                }
            }
        }

        if ((inversionCount + blankbuttonNumber) % 2 != 0) {
            return createPuzzle()
        }

        return shuffledNumbers;
    }
}