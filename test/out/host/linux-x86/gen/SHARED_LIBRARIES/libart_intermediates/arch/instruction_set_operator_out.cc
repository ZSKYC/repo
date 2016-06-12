#include <iostream>

#include "arch/instruction_set.h"

// This was automatically generated by art/tools/generate-operator-out.py --- do not edit!
namespace art {
std::ostream& operator<<(std::ostream& os, const InstructionSet& rhs) {
  switch (rhs) {
    case kNone: os << "None"; break;
    case kArm: os << "Arm"; break;
    case kArm64: os << "Arm64"; break;
    case kThumb2: os << "Thumb2"; break;
    case kX86: os << "X86"; break;
    case kX86_64: os << "X86_64"; break;
    case kMips: os << "Mips"; break;
    case kMips64: os << "Mips64"; break;
    default: os << "InstructionSet[" << static_cast<int>(rhs) << "]"; break;
  }
  return os;
}
}  // namespace art
