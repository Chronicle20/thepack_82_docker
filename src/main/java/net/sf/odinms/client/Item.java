package net.sf.odinms.client;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class Item implements IItem {

	private final int id;
	private byte position;
	private short quantity;
	private final int petId;
	private String owner = "";
	protected List<String> log;

	public Item (int id, byte position, short quantity) {
		super();
		this.id = id;
		this.position = position;
		this.quantity = quantity;
		this.petId = -1;
		this.log = new LinkedList<>();
	}
	
	public Item(int id, byte position, short quantity, int petid) {
		super();
		this.id = id;
		this.position = position;
		this.quantity = quantity;
		this.petId = petid;
		this.log = new LinkedList<>();
	}

	public IItem copy() {
		Item ret = new Item(id, position, quantity, petId);
		ret.owner = owner;
		ret.log = new LinkedList<>(log);
		return ret;
	}

	public void setPosition(byte position) {
		this.position = position;
	}

	public void setQuantity(short quantity) {
		this.quantity = quantity;
	}

	@Override
	public int getItemId() {
		return id;
	}

	@Override
	public byte getPosition() {
		return position;
	}

	@Override
	public short getQuantity() {
		return quantity;
	}

	@Override
	public byte getType() {
		return ITEM;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	@Override
	public int getPetId() {
		return petId;
	}

	@Override
	public int compareTo(IItem other) {
		return Integer.compare(Math.abs(position), Math.abs(other.getPosition()));
	}

	@Override
	public String toString() {
		return "Item: " + id + " quantity: " + quantity;
	}

	public List<String> getLog() {
		return Collections.unmodifiableList(log);
	}

    public void log(String msg, boolean fromDB) {
    }
}
 
