package enshud.s3.checker;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Checker {

	/**
	 * サンプルmainメソッド．
	 * 単体テストの対象ではないので自由に改変しても良い．
	 */
	public static void main(final String[] args) {
		// normalの確認
		new Checker().run("data/ts/normal17.ts");
		new Checker().run("data/ts/normal10.ts");

		// semerrの確認
		new Checker().run("data/ts/semerr05.ts");
		new Checker().run("data/ts/semerr07.ts");

		// synerrの確認
		new Checker().run("data/ts/synerr01.ts");
		new Checker().run("data/ts/synerr02.ts");
	}

	/**
	 * TODO
	 *
	 * 開発対象となるChecker実行メソッド．
	 * 以下の仕様を満たすこと．
	 *
	 * 仕様:
	 * 第一引数で指定されたtsファイルを読み込み，意味解析を行う．
	 * 意味的に正しい場合は標準出力に"OK"を，正しくない場合は"Semantic error: line"という文字列とともに，
	 * 最初のエラーを見つけた行の番号を標準エラーに出力すること （例: "Semantic error: line 6"）．
	 * また，構文的なエラーが含まれる場合もエラーメッセージを表示すること（例： "Syntax error: line 1"）．
	 * 入力ファイル内に複数のエラーが含まれる場合は，最初に見つけたエラーのみを出力すること．
	 * 入力ファイルが見つからない場合は標準エラーに"File not found"と出力して終了すること．
	 *
	 * @param inputFileName 入力tsファイル名
	 */
	Tokens tokens =new Tokens();
	SymbolTable symbolTable = new SymbolTable();
	SymbolTable subSymbolTable;
	boolean isSub = false;
	boolean isNew = false;
	public void run(final String inputFileName) {
		try {
			tokens.buildTokens(inputFileName);
			program();
			tokens.isEnd();
			System.out.println("OK");
		}catch (IOException e) { // 例外をつかむ．
			System.err.println("File not found"); // err出力
		}catch (EnshudSyntaxError e) { // 例外をつかむ．
			System.err.println("Syntax error: line " + e.getLineNumber()); // 行番号と共にerr出力
		}catch (EnshudSemanticError e) { // 例外をつかむ．
			System.err.println("Semantic error: line " + e.getLineNumber()); // 行番号と共にerr出力
		}
	}

	public class Tokens{
		private int counter;
		private int tokenNum;
		private ArrayList<Token> tokenSet = new ArrayList<>();

		public void buildTokens(final String inputFileName) throws IOException {
			this.counter = -1;
			Path path = Paths.get(inputFileName);
			Charset charset = StandardCharsets.UTF_8;
			BufferedReader reader = Files.newBufferedReader(path, charset);
			String line;
			while ((line = reader.readLine()) != null) {
				Token element = new Token(line);
				tokenSet.add(element);
			}
			tokenNum = tokenSet.size();
		}

		public String next() {
			return tokenSet.get(++counter).getToken();
		}
		public String getNextID() {
			return tokenSet.get(counter+1).getID();
		}
		public String getNextToken() {
			return tokenSet.get(counter+1).getToken();
		}
		public String getCurrentLineNumber() {
			return tokenSet.get(counter).getLineNum();
		}
		public String getCurrentToken() {
			return tokenSet.get(counter).getToken();
		}
		public void inc() {
			this.counter++;
		}
		public void isEnd() throws EnshudSyntaxError{
			if(this.counter < this.tokenNum -1 ) {
				this.counter++;
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	public class Token{
		String tokenInfo[] = new String[4];

		Token(String line){
			int end = line.lastIndexOf("'");
			if(end == -1) {
				this.tokenInfo = line.split("\t");
			}else {
				String tmp[] = new String[3];
				this.tokenInfo[0] = line.substring(0, end+1);
				tmp = line.substring(end+2).split("\t");
				for(int i = 1; i < 4; i++) {
					this.tokenInfo[i] = tmp[i-1];
				}
			}
		}

		public String getToken() {
			return tokenInfo[0];
		}
		public String getID() {
			return tokenInfo[2];
		}
		public String getLineNum() {
			return tokenInfo[3];
		}
	}

	class EnshudSyntaxError extends Exception{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		String lineNumber; // 何行目で構文エラーが起きたかを保持する．

		EnshudSyntaxError(String lineNumber) {
			this.lineNumber = lineNumber; // コンストラクタで行番号を保持．
		}

		private String getLineNumber() {
			return lineNumber;
		}
	}

	class EnshudSemanticError extends Exception{
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		String lineNumber; // 何行目で意味エラーが起きたかを保持する．

		EnshudSemanticError(String lineNumber) {
			this.lineNumber = lineNumber; // コンストラクタで行番号を保持．
		}

		private String getLineNumber() {
			return lineNumber;
		}
	}

	public class SymbolTable{
		private ArrayList<Symbol> symbolSet = new ArrayList<>();
		private int start = 0, end = 0;
		private boolean isArr = false;

		public void addVar(String name) throws EnshudSemanticError {
			if(isSymbol(name)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			symbolSet.add(new Symbol(name));
			this.end++;
		}
		public void addFunc(String name) throws EnshudSemanticError {
			if(isSymbol(name)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			Symbol subFunc = new Symbol(name);
			subFunc.setFunc();
			symbolSet.add(subFunc);
			this.end++;
		}
		public void setArr() {
			this.isArr = true;
		}
		public void addType(String type) {
			while(this.start != this.end) {
				symbolSet.get(this.start).setType(type);
				if(this.isArr) {
					symbolSet.get(this.start).setArr();
				}
				start++;
			}
			this.isArr = false;
		}
		public boolean isSymbol(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return true;
			}
			return false;
		}
		public String symbolType(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name))
					return symbolSet.get(i).getType();
			}
			return null;
		}
		public boolean isArr(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name) && symbolSet.get(i).isArr()) {
					return true;
				}
			}
			return false;
		}
		public boolean isFunc(String name) {
			for(int i = 0; i < this.end; i++) {
				if(symbolSet.get(i).getName().equals(name) && symbolSet.get(i).isFunc()) {
					return true;
				}
			}
			return false;
		}
	}

	private class Symbol{
		private String name;
		protected String type;
		private boolean isArr = false;
		private boolean isFunc = false;

		Symbol(String name){
			this.name = name;
		}
		public String getName() {
			return this.name;
		}
		public String getType() {
			return this.type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public void setArr() {
			this.isArr = true;
		}
		public void setFunc() {
			this.isFunc = true;
		}
		public boolean isArr() {
			return this.isArr;
		}
		public boolean isFunc() {
			return this.isFunc;
		}
	}


	void program() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("program")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		programName();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		block();

		complexStatement();

		if(! tokens.next().equals(".")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void programName()  throws EnshudSyntaxError{
		identifier();
	}

	void block() throws EnshudSyntaxError, EnshudSemanticError{
		variableDeclaration();

		isSub = true;
		subprograms();
		isSub = false;
	}

	void variableDeclaration() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.getNextID().equals("21")){
			return;
		}
		tokens.inc();

		isNew = true;
		variableDeclarations();
		isNew = false;
	}

	void variableDeclarations() throws EnshudSyntaxError, EnshudSemanticError{
		variableNames();

		if(! tokens.next().equals(":")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		type();

		if(! tokens.next().equals(";")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(tokens.getNextID().equals("43")) {
			variableDeclarations();
		}
	}

	void variableNames() throws EnshudSyntaxError, EnshudSemanticError{
		variableName();

		if(isSub) {
			if(subSymbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			subSymbolTable.addVar(tokens.getCurrentToken());
		}else {
			if(symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			symbolTable.addVar(tokens.getCurrentToken());
		}

		if(tokens.getNextID().equals("41")) {
			tokens.inc();
			variableNames();
		}
	}

	void variableName() throws EnshudSyntaxError{
		identifier();
	}

	void type() throws EnshudSyntaxError, EnshudSemanticError {
		if(tokens.getNextID().equals("1")) {
			array();
		}else {
			standard();
		}
	}

	void standard() throws EnshudSyntaxError{
		String type = tokens.next();
		if(! (type.equals("char")||type.equals("integer")||type.equals("boolean"))) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		if(isSub) {
			subSymbolTable.addType(tokens.getCurrentToken());
		}
		else {
			symbolTable.addType(tokens.getCurrentToken());
		}
	}

	void array() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("array")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("[")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		int min = min();

		if(! tokens.next().equals("..")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(max() < min) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("]")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals("of")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		if(isSub) {
			subSymbolTable.setArr();
		}
		else {
			symbolTable.setArr();
		}
		standard();
	}

	int max() throws EnshudSyntaxError, EnshudSemanticError{
		return digit();
	}

	int min() throws EnshudSyntaxError, EnshudSemanticError{
		return digit();
	}

	int digit() throws EnshudSyntaxError{
		boolean isMinus = sign();

		int num = unsignedDigit();
		if(isMinus) {
			return -num;
		}
		return num;
	}

	boolean sign() {
		if(tokens.getNextID().equals("31")) {
			tokens.inc();
			return true;
		}else if(tokens.getNextID().equals("30")) {
			tokens.inc();
		}
		return false;
	}

	void subprograms() throws EnshudSyntaxError, EnshudSemanticError {
		while(tokens.getNextID().equals("16")) {
			subSymbolTable = new SymbolTable();
			subprogram();

			if(! tokens.next().equals(";")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void subprogram() throws EnshudSyntaxError, EnshudSemanticError {
		isNew = true;
		subprogramHead();

		variableDeclaration();
		isNew = false;

		complexStatement();
	}

	void subprogramHead() throws EnshudSyntaxError, EnshudSemanticError {
		if(! tokens.next().equals("procedure")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		procedureName();

		formalParameter();

		if(! tokens.next().equals(";")) {
			System.out.println(tokens.getCurrentToken());
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void procedureName() throws EnshudSyntaxError, EnshudSemanticError {
		identifier();
		if(isNew) {
			if(symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			else {
				symbolTable.addFunc(tokens.getCurrentToken());
			}
		}
	}

	void formalParameter() throws EnshudSyntaxError, EnshudSemanticError {
		if(! tokens.getNextID().equals("33")){
			return;
		}
		tokens.inc();

		formalParameters();

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void formalParameters() throws EnshudSyntaxError, EnshudSemanticError {
		formalParameterNames();

		if(! tokens.next().equals(":")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		standard();

		while(tokens.getNextID().equals("37")) {
			tokens.inc();
			formalParameters();
		}
	}

	void formalParameterNames() throws EnshudSyntaxError, EnshudSemanticError {
		formalParameterName();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			formalParameterName();
		}
	}

	void formalParameterName() throws EnshudSyntaxError, EnshudSemanticError{
		identifier();
		if(subSymbolTable.isSymbol(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		subSymbolTable.addVar(tokens.getCurrentToken());
	}

	void complexStatement() throws EnshudSyntaxError, EnshudSemanticError{
		if(! tokens.next().equals("begin")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		statements();

		if(! tokens.next().equals("end")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void statements() throws EnshudSyntaxError, EnshudSemanticError{
		statement();

		if(! tokens.next().equals(";")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}

		while(! tokens.getNextID().equals("8")) {
			statement();

			if(! tokens.next().equals(";")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}
	}

	void statement() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("10")) {
			tokens.inc();

			if(! expression().equals("boolean")){
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}

			if(! tokens.next().equals("then")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			complexStatement();

			ifSecondHalf();
		}else if(tokens.getNextID().equals("22")) {
			tokens.inc();

			if(! expression().equals("boolean")){
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}

			if(! tokens.next().equals("do")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}

			complexStatement();
		}else {
			simpleStatement();
		}
	}

	void ifSecondHalf() throws EnshudSyntaxError, EnshudSemanticError {
		if(! tokens.getNextID().equals("7")){
			return;
		}
		tokens.inc();

		complexStatement();
	}

	void simpleStatement() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("18") ||tokens.getNextID().equals("23") ) {
			inOrOutput();
		}else if(tokens.getNextID().equals("2")) {
			complexStatement();
		}else if(symbolTable.isFunc(tokens.getNextToken())) {
			procedureStatement();
		}else {
			assignmentStatement();
		}
	}

	String variable() throws EnshudSyntaxError, EnshudSemanticError{
		String type;

		variableName();

		if(isSub) {
			if(! (subSymbolTable.isSymbol(tokens.getCurrentToken()) || symbolTable.isSymbol(tokens.getCurrentToken()))) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			if((type = subSymbolTable.symbolType(tokens.getCurrentToken())) == null) {
				type = symbolTable.symbolType(tokens.getCurrentToken());
			}
			if(subSymbolTable.isArr(tokens.getCurrentToken()) || symbolTable.isArr(tokens.getCurrentToken())) {
				variableSecondHalf();
			}
		}else {
			if(! symbolTable.isSymbol(tokens.getCurrentToken())) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			type = symbolTable.symbolType(tokens.getCurrentToken());
			if(symbolTable.isArr(tokens.getCurrentToken())) {
				variableSecondHalf();
			}
		}
		return type;
	}

	void variableSecondHalf() throws EnshudSyntaxError, EnshudSemanticError {
		if(!tokens.getNextID().equals("35")) {
			return;
		}
		tokens.inc();
		if(! expression().equals("integer")){
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		if(! tokens.next().equals("]")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void assignmentStatement() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = variable();

		if(symbolTable.isArr(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
		else if(isSub && subSymbolTable.isArr(tokens.getCurrentToken())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}

		if(! tokens.next().equals(":=")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		if(! type1.equals(expression())) {
			throw new EnshudSemanticError(tokens.getCurrentLineNumber());
		}
	}

	void procedureStatement() throws EnshudSyntaxError, EnshudSemanticError{
		procedureName();

		if(! tokens.getNextID().equals("33")) {
			return;
		}
		tokens.inc();

		expressions();

		if(! tokens.next().equals(")")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void expressions() throws EnshudSyntaxError, EnshudSemanticError{
		expression();

		while(tokens.getNextID().equals("41")) {
			tokens.inc();

			expression();
		}
	}

	String expression() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = simpleExpression();

		if(relationalOperator()) {
			String type2 = simpleExpression();
			if(!type1.equals(type2)) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
			return "boolean";
		}
		return type1;
	}

	boolean relationalOperator() {//関係演算子を統合
		if(tokens.getNextID().equals("24") || tokens.getNextID().equals("25") || tokens.getNextID().equals("26") || tokens.getNextID().equals("27") || tokens.getNextID().equals("28") || tokens.getNextID().equals("29")) {
			tokens.inc();
			return true;
		}
		return false;
	}

	String simpleExpression() throws EnshudSyntaxError, EnshudSemanticError{
		boolean isMinus = sign();
		String type1 = term();
		String op;
		if(isMinus && !type1.equals("integer")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		while((op = additionOperator()) != null) {
			String type2 = term();
			if(!(type1.equals(op)  && type1.equals(type2))) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
		}
		return type1;
	}

	String additionOperator() {//加法演算子を統合
		if(tokens.getNextID().equals("30") || tokens.getNextID().equals("31") ) {
			tokens.inc();
			return "integer";
		}else if(tokens.getNextID().equals("15")) {
			tokens.inc();
			return "boolean";
		}
		return null;
	}

	String term() throws EnshudSyntaxError, EnshudSemanticError{
		String type1 = factor();
		String op;

		while((op = multiplicationOperator()) != null) {
			String type2 = factor();
			if(!(type1.equals(op)  && type1.equals(type2))) {
				throw new EnshudSemanticError(tokens.getCurrentLineNumber());
			}
		}
		return type1;
	}

	String multiplicationOperator() {//乗法演算子を統合
		if(tokens.getNextID().equals("5") || tokens.getNextID().equals("12") || tokens.getNextID().equals("32")) {
			tokens.inc();
			return "integer";
		}else if(tokens.getNextID().equals("0")) {
			tokens.inc();
			return "boolean";
		}
		return null;
	}

	String factor() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("43")) {
			return variable();
		}
		else if(tokens.getNextID().equals("33")) {
			tokens.inc();

			String type = expression();

			if(! tokens.next().equals(")")) {
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
			return type;
		}else if(tokens.getNextID().equals("13")) {
			tokens.inc();
			return factor();
		}else {
			return constant();
		}
	}

	void inOrOutput() throws EnshudSyntaxError, EnshudSemanticError{
		if(tokens.getNextID().equals("18")) {
			tokens.inc();
			if( !tokens.getNextID().equals("33")) {
				return;
			}
			tokens.inc();

			variables();

			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else if(tokens.next().equals("writeln")){
			if(! tokens.getNextID().equals("33")) {
				return;
			}
			tokens.inc();
			expressions();

			if(! tokens.next().equals(")")){
				throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
			}
		}else {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
	}

	void variables() throws EnshudSyntaxError, EnshudSemanticError{
		variable();
		while(tokens.getNextID().equals("41")) {
			tokens.inc();
			variable();
		}
	}

	String constant() throws EnshudSyntaxError{
		if(tokens.getNextID().equals("9") || tokens.getNextID().equals("20")) { //trueとfalseを統合
			tokens.inc();
			return "boolean";
		}else if(tokens.getNextID().equals("45")) { //文字列処理を統合
			string();
			return "char";
		}else {
			unsignedDigit();
			return "integer";
		}
	}
	int unsignedDigit() throws EnshudSyntaxError {
		if(! tokens.getNextID().equals("44")) {
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		return Integer.parseInt(tokens.next());

	}

	void string() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("45")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
	}

	void identifier() throws EnshudSyntaxError{
		if(! tokens.getNextID().equals("43")){
			throw new EnshudSyntaxError(tokens.getCurrentLineNumber());
		}
		tokens.inc();
	}
}
