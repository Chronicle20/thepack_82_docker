package net.sf.odinms.client;

public record PetCommand(int petId, int skillId, int probability, int increase) {
}
