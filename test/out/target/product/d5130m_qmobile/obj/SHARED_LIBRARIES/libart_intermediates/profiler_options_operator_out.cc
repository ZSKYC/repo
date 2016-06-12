#include <iostream>

#include "profiler_options.h"

// This was automatically generated by art/tools/generate-operator-out.py --- do not edit!
namespace art {
std::ostream& operator<<(std::ostream& os, const ProfileDataType& rhs) {
  switch (rhs) {
    case kProfilerMethod: os << "ProfilerMethod"; break;
    case kProfilerBoundedStack: os << "ProfilerBoundedStack"; break;
    default: os << "ProfileDataType[" << static_cast<int>(rhs) << "]"; break;
  }
  return os;
}
}  // namespace art
