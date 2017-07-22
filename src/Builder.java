import bwapi.Unit;
import bwapi.UnitType;

public class Builder{
	private Unit builder;
	private UnitType building;
	public Builder(Unit builder, UnitType building) {
		this.builder = builder;
		this.building = building;
	}
	public void setBuilder(Unit builder) {
		this.builder = builder;
	}
	public Unit getBuilder(){
		return this.builder;
	}
	
	public void setBuilding(UnitType building) {
		this.building = building;
	}
	
	public UnitType getBuilding() {
		return this.building;
	}
}
