#include "testUtils.h"
#include <cmath>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <map>
#include <sstream>
#include <cstring>
#include <vector>

bool beginsWithNumber(std::string const & str) {
  if (str.length() == 0) {
    return false;
  }
  return isdigit(str[0]);
}

double str2double (std::string & str) {
  char * cstr = new char[str.length() + 1];
  strcpy(cstr, str.c_str());
  char * cstr_end = cstr;
  double x = strtod(cstr, &cstr_end);
  if(cstr == cstr_end) {
    //you have an error!
    return 0;
  } else {
    return x;
  }
}

std::string trim(const std::string& str)
{
  const std::string& whitespace = " \t\"";
  const auto strBegin = str.find_first_not_of(whitespace);
  if (strBegin == std::string::npos)
    return ""; // no content

  const auto strEnd = str.find_last_not_of(whitespace);
  const auto strRange = strEnd - strBegin + 1;

  return str.substr(strBegin, strRange);
}

int parseCSVFile(std::string filename,
                 std::vector<std::string> & csvHeaders,
                 std::vector<std::map<std::string, std::string>> & csvStringRows,
                 std::vector<std::map<std::string, double>> & csvDoubleRows) {

  std::ifstream input{filename};

  if (!input.is_open()) {
    std::cerr << "Couldn't read file: " << filename << "\n";
    return 1;
  }

  // Parse header row
  std::string line;
  std::getline(input, line);
  std::istringstream ss(std::move(line));
  std::vector<std::string> headerRow;
  for (std::string value; std::getline(ss, value, ',');) {
    csvHeaders.push_back(std::move(value));
  }

  // Parse value rows
  int rowIndex = 0;
  for (std::string line; std::getline(input, line);) {
    std::istringstream ss(std::move(line));
    std::map<std::string, std::string> strmap;
    std::map<std::string, double> doublemap;
    int colIndex = 0;
    // std::getline can split on other characters, here we use ','
    for (std::string value; std::getline(ss, value, ',');) {
      std::string header = csvHeaders[colIndex];
      if (beginsWithNumber(value)) {
        doublemap[header] = str2double(value);
      } else {
        strmap[header] = trim(value);
      }
      colIndex++;
    }

    csvStringRows.push_back(strmap);
    csvDoubleRows.push_back(doublemap);
    rowIndex++;
  }

  return 0;
}

void printCSVData(std::vector<std::string> csvHeaders,
                  std::vector<std::map<std::string, std::string>> csvStringRows,
                  std::vector<std::map<std::string, double>> csvDoubleRows) {
  // Print out our table
  for (const std::string & value : csvHeaders) {
    std::cout << std::setw(10) << value;
    std::cout << "\t";
  }
  std::cout << "\n";

  // Print out our table
  for (int i = 0; i < csvStringRows.size(); i++) {
    for (std::string header : csvHeaders) {
      std::map<std::string, double> doubleRow = csvDoubleRows[i];
      std::map<std::string, std::string> strRow = csvStringRows[i];

      if (doubleRow.count(header)) {
        std::cout << std::setw(10) << doubleRow[header];
      } else if (strRow.count(header)) {
        std::cout << std::setw(10) << strRow[header];
      }
      std::cout << "\t";
    }
    std::cout << "\n";
  }
}

bool areClose(const double observed, const double expected, const double epsilon)
{
  return fabs(observed - expected) < epsilon;
}

double roundToSixDecimalPlaces(const double numberToBeRounded)
{
  std::stringstream ss;
  ss << std::fixed;
  ss.precision(6); // set to 6 places after decimal
  ss << numberToBeRounded; // put number to be rounded into the stringstream
  std::string s = ss.str(); // convert stringstream to string
  double roundedValue = stod(s); // convert string to double
  return roundedValue;
}

void reportTestResult(int row, struct TestInfo& testInfo, const std::string testName, const double observed, const double expected, const double epsilon)
{
  testInfo.numTotalTests++;
  if(areClose(observed, expected, epsilon)) {
    //std::cout << testName << "\npassed successfully\n";
    testInfo.numPassed++;
  } else {
    std::cout << testName << "\n(Row: " << row << ")" << "\nfailed\nobserved value " << observed << " differs from expected value " << expected << " by more than " << epsilon << "\n\n";
    testInfo.numFailed++;
  }
}
