import java.io.*;
import java.util.*;

public class Processor {
	static PrintWriter pw = new PrintWriter(System.out);
	// Test cases
	static byte registerFile[] = new byte[64];// {22,16,87,54,102,434,33,67,98,49};
	static String instructionMemory[] = new String[1024];
	static byte dataMemory[] = new byte[2048];
	static int statusRegister[] = new int[8];
	static int pc = 0;
	static int numberOfInstructions = 0;
	static String currentInstruction = "";
	static int opcode;
	static int r1;
	static int r2;
	static byte valueR1;
	static byte valueR2;
	static int imm;
	static int address;
	static int branch = 0;
	static int branchPc = 0;
	static boolean branchCond;
	static int immCond;
	static int addressOfBranch=0;
	static int cycles;
	static int pipelined;
	public static void main(String[] args) throws IOException {
		parser("Program_1.txt");
		 cycles = 3 + (numberOfInstructions - 1);
		int i = 1; // cycles counter
		while (i <= cycles) {
			System.out.println("Cycle: " + i);
			System.out.println("PC: " + pc);
			if (i == 1 || branch == 1) {
				fetch();
				branch = branch == 1 ? 2 : 0;
			}else if (i == 2 || branch == 2) {
				decode(currentInstruction);
				System.out.println("Instruction " + (pc - 1) + " Decoded");
				fetch();
				branch = 0;
			} else {
				execute(opcode, r1, valueR1, valueR2, imm, address);
				System.out.println("Instruction " + (pc-2) + " Executed");
				if (i!=cycles) {//i <= numberOfInstructions + 1
					decode(currentInstruction);// 2
					System.out.println("Instruction " + (pc-1) + " Decoded");
				}
				if (i!=cycles-1 && i!=cycles )//i <= numberOfInstructions
					fetch();
			}
			if (branch == 1)
				pc = branchPc;
			else 
				//if(pc<= numberOfInstructions-1)
					pc++;
			System.out.println("PC was updated to " + pc);
			i++;
			System.out.println();
		}
		System.out.println("Program Ended");
		System.out.println("PC: " + pc);
		getRegisterFile();
		getStatusRegister();
		getDataMem();
		getInstMem();
	}

	public static void fetch() {
		if(instructionMemory[pc]!=null) {
			currentInstruction = instructionMemory[pc];
			System.out.println("Instruction " + pc + " Fetched");
		}
		else 
			return;
	}
	public static void decode(String instruction) {
		opcode = Integer.parseInt(instruction.substring(0, 4), 2);
		r1 = Integer.parseInt(instruction.substring(4, 10), 2);
		r2 = Integer.parseInt(instruction.substring(10), 2);
		if (instruction.charAt(10) == '1')
			imm = (int) Long.parseLong("1111111111111111111111111111111111111111" + instruction.substring(10), 2);
		else
			imm = r2;

		address = r2;
		valueR1 = registerFile[r1];
		valueR2 = registerFile[r2];
		System.out.println("Decoded :" +instruction +" as input for next stage: ");
		System.out.println("Opcode: " + opcode);
		System.out.println("R1: " + r1);
		System.out.println("R1 Value: " + valueR1);
		System.out.println("R2 Value: " + valueR2);
		System.out.println("Immediate: " + imm);
		System.out.println("Address: " + address);
	}

	public static void execute(int opcode, int r1, byte valueR1, byte valueR2, int imm, int address) {
		System.out.println("Executing instruction with ");
		System.out.println("Opcode: " + opcode);
		System.out.println("R1: " + r1);
		System.out.println("R1 Value: " + valueR1);
		System.out.println("R2 Value: " + valueR2);
		System.out.println("Immediate: " + imm);
		System.out.println("Address: " + address);
		switch (opcode) {
		case 0:
			byte add = (byte) (valueR1 + valueR2);
			registerFile[r1] = add;
			getRegisterFile();
			int temp1 = valueR1 & 0x000000FF;
			int temp2 = valueR2 & 0x000000FF;
			if (((temp1 + temp2) & 0b00000000000000000000000100000000) == 0b00000000000000000000000100000000) {
				statusRegister[4] = 1;
			} else {
				statusRegister[4] = 0;
			}
			if (valueR1 > 0 && valueR2 > 0 && (add < 0))
				statusRegister[3] = 1;
			else if (valueR1 < 0 && valueR2 < 0 && (add > 0))
				statusRegister[3] = 1;
			else
				statusRegister[3] = 0;

			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;

			statusRegister[1] = statusRegister[2] ^ statusRegister[3];
			getStatusRegister();
			break;
		case 1:
			byte sub = (byte) (valueR1 - valueR2);
			registerFile[r1] = sub;
			getRegisterFile();
			if (valueR1 > 0 && valueR2 < 0 && (sub < 0))
				statusRegister[3] = 1;
			else if (valueR1 < 0 && valueR2 > 0 && (sub > 0))
				statusRegister[3] = 1;
			else
				statusRegister[3] = 0;

			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;

			statusRegister[1] = statusRegister[2] ^ statusRegister[3];
			getStatusRegister();
			break;
		case 2:
			registerFile[r1] = (byte) (valueR1 * valueR2); // 0000 0011
			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;
			getRegisterFile();
			getStatusRegister();
			break;
		case 3:
			registerFile[r1] = (byte) imm;
			getRegisterFile();
			break;
		case 4:
			if (valueR1 == 0) {
				addressOfBranch=pc-2;
				branchPc=addressOfBranch+1+imm;
				branch=1;
				branchCond=true;
				immCond=imm;
				int skipped = branchPc-addressOfBranch-1-2;
				pipelined = numberOfInstructions -skipped;
				cycles =3+ (pipelined-1);
			}
			break;
		case 5:
			registerFile[r1] = (byte) (valueR1 & imm);
			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;
			getRegisterFile();
			getStatusRegister();
			break;
		case 6:
			registerFile[r1] = (byte) (valueR1 ^ valueR2);
			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;
			getRegisterFile();
			getStatusRegister();
			break;
		case 7:
			// PC = R1 || R2
			// ValueR1 = 0000000000000001
			addressOfBranch=pc-2;
			String s1 = String.format("%8s", Integer.toBinaryString(valueR1 & 0xFF)).replace(' ', '0');
			String s2 = String.format("%8s", Integer.toBinaryString(valueR2 & 0xFF)).replace(' ', '0');
			String s = s1 + s2;
			branchPc = Integer.parseInt(s, 2);
			branch = 1;
			// pc = Integer.parseInt(s.substring(6), 2);
			int skipped = branchPc-1-addressOfBranch-2;
			pipelined = numberOfInstructions -skipped;
			cycles =3+ (pipelined-1);
			break;
		case 8:
			registerFile[r1] = (byte) (valueR1 << imm);
			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;
			getRegisterFile();
			getStatusRegister();
			break;
		case 9:
			registerFile[r1] = (byte) (valueR1 >> imm);
			statusRegister[2] = registerFile[r1] < 0 ? 1 : 0;
			statusRegister[0] = registerFile[r1] == 0 ? 1 : 0;
			getRegisterFile();
			getStatusRegister();
			break;
		case 10:
			registerFile[r1] = dataMemory[address];
			getRegisterFile();
			break;
		case 11:
			dataMemory[address] = valueR1;
			getDataMem();
			break;
		default:
			System.out.println("Hello");
		}
	}

	public static void parser(String fileName) throws IOException {
		File file = new File(fileName);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		String line[];
		String instruction = "";
		while ((st = br.readLine()) != null) {
			line = st.split(" ");
			instruction = "";
			String binaryString = Integer.toBinaryString(Integer.parseInt(line[1].substring(1))); // R11->"11"->11->"1011"->
			String withLeadingZeros = String.format("%6s", binaryString).replace(' ', '0');// "1011"->"001011"
			instruction += withLeadingZeros;

			if (line[0].equals("ADD")) {
				instruction = "0000" + instruction;
				instruction += addR(line[2]);
			} else if (line[0].equals("SUB")) {
				instruction = "0001" + instruction;
				instruction += addR(line[2]);
			} else if (line[0].equals("MUL")) {
				instruction = "0010" + instruction;
				instruction += addR(line[2]);
			} else if (line[0].equals("MOVI")) {
				instruction = "0011" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("BEQZ")) {
				instruction = "0100" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("ANDI")) {
				instruction = "0101" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("EOR")) {
				instruction = "0110" + instruction;
				instruction += addR(line[2]);
			} else if (line[0].equals("BR")) {
				instruction = "0111" + instruction;
				instruction += addR(line[2]);
			} else if (line[0].equals("SAL")) {
				instruction = "1000" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("SAR")) {
				instruction = "1001" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("LDR")) {
				instruction = "1010" + instruction;
				instruction += addImm(line[2]);
			} else if (line[0].equals("STR")) {
				instruction = "1011" + instruction;
				instruction += addImm(line[2]);
			}

			instructionMemory[numberOfInstructions++] = instruction;
		}
	}

	public static String addImm(String line) {
		String binaryString = Integer.toBinaryString(Integer.parseInt(line) & 0b111111); // 11->"11"->11->"1011"->
		// SHOULD BE REMOVED HAS NO EFFECT
		String withLeadingZeros = String.format("%6s", binaryString).replace(' ', '0');// "1011"->"001011"
		return withLeadingZeros;
	}

	public static String addR(String line) {
		String binaryString = Integer.toBinaryString(Integer.parseInt(line.substring(1))); // R11->"11"->11->"1011"->
		String withLeadingZeros = String.format("%6s", binaryString).replace(' ', '0');// "1011"->"001011"
		return withLeadingZeros;
	}

	public static void getRegisterFile() {
		System.out.print("RegisterFile: ");
		for (byte x : registerFile) {
			System.out.print(x + " ");
		}
		System.out.println();
	}

	public static void getStatusRegister() {
		System.out.println("                 " + "Z " + "S " + "N " + "V " + "C");
		System.out.print("Status Register: ");
		for (int x : statusRegister) {
			System.out.print(x + " ");
		}
		System.out.println();
	}

	public static void getDataMem() {
		System.out.print("Data Memory: ");
		for (byte x : dataMemory) {
			System.out.print(x + " ");
		}
		System.out.println();
	}

	public static void getInstMem() {
		System.out.print("Instruction Memory: ");
		for (String x : instructionMemory) {
			System.out.print(x + " ");
		}
		System.out.println();
	}
}