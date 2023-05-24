package dmeeuwis.kanjimaster.logic.drawing;

class StrokeCriticism {
    final public Integer cost;
    final public String message;

	public StrokeCriticism(String message) {
		this.message = message;
		this.cost = 1;
	}

	public StrokeCriticism(String message, int cost) {
		this.message = message;
		this.cost = cost;
	}
}
