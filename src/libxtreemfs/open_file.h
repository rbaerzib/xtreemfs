// Copyright (c) 2010 Minor Gordon
// All rights reserved
// 
// This source file is part of the XtreemFS project.
// It is licensed under the New BSD license:
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
// * Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
// * Neither the name of the XtreemFS project nor the
// names of its contributors may be used to endorse or promote products
// derived from this software without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL Minor Gordon BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#ifndef _LIBXTREEMFS_OPEN_FILE_H_
#define _LIBXTREEMFS_OPEN_FILE_H_

#include "shared_file.h"


namespace xtreemfs
{
  class OpenFile : public YIELD::platform::File
  {
  public:
    const org::xtreemfs::interfaces::XCap& get_xcap() const { return xcap; }

    // yidl::runtime::Object
    YIDL_RUNTIME_OBJECT_PROTOTYPES( OpenFile, 0 );

    // YIELD::platform::File
    YIELD_PLATFORM_FILE_PROTOTYPES;
    size_t getpagesize();

  private:
    friend class SharedFile;
    class XCapTimer;
    friend class XCapTimer;

    OpenFile
    (
      auto_SharedFile parent_shared_file,
      const org::xtreemfs::interfaces::XCap& xcap
    );

    ~OpenFile();

    bool closed;
    auto_SharedFile parent_shared_file;
    org::xtreemfs::interfaces::XCap xcap;
    yidl::runtime::auto_Object<XCapTimer> xcap_timer;
  };

  typedef yidl::runtime::auto_Object<OpenFile> auto_OpenFile;
};

#endif